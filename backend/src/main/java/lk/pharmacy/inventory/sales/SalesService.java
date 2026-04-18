package lk.pharmacy.inventory.sales;

import lk.pharmacy.inventory.domain.Medicine;
import lk.pharmacy.inventory.domain.Sale;
import lk.pharmacy.inventory.domain.SaleItem;
import lk.pharmacy.inventory.domain.User;
import lk.pharmacy.inventory.exception.ApiException;
import lk.pharmacy.inventory.repo.MedicineRepository;
import lk.pharmacy.inventory.repo.SaleItemRepository;
import lk.pharmacy.inventory.repo.SaleRepository;
import lk.pharmacy.inventory.sales.dto.*;
import lk.pharmacy.inventory.util.CurrentUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;

@Service
public class SalesService {

    private final MedicineRepository medicineRepository;
    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final CurrentUserService currentUserService;

    public SalesService(MedicineRepository medicineRepository,
                        SaleRepository saleRepository,
                        SaleItemRepository saleItemRepository,
                        CurrentUserService currentUserService) {
        this.medicineRepository = medicineRepository;
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public SaleBillResponse createSale(CreateSaleRequest request) {
        if (request.items() == null || request.items().isEmpty()) {
            throw new ApiException("At least one sale item is required");
        }

        User currentUser = currentUserService.getCurrentUser();
        Long tenantId = currentUser.getTenant().getId();
        Long pharmacyId = currentUserService.getCurrentPharmacy().getId();
        Sale sale = new Sale();
        sale.setTransactionId(generateTransactionId());
        sale.setTenant(currentUser.getTenant());
        sale.setPharmacy(currentUserService.getCurrentPharmacy());
        sale.setCreatedBy(currentUser);
        sale.setCustomerName(trimToNull(request.customerName()));
        sale.setCustomerPhone(trimToNull(request.customerPhone()));

        BigDecimal beforeDiscount = BigDecimal.ZERO;

        for (SaleItemRequest itemRequest : request.items()) {
            Medicine medicine = medicineRepository.findByIdAndTenant_IdAndPharmacy_Id(itemRequest.medicineId(), tenantId, pharmacyId)
                    .orElseThrow(() -> new ApiException("Medicine not found: " + itemRequest.medicineId()));

            if (medicine.getQuantity() < itemRequest.quantity()) {
                throw new ApiException("Insufficient stock for medicine: " + medicine.getName());
            }

            boolean allowOverride = Boolean.TRUE.equals(itemRequest.allowPriceOverride());
            BigDecimal inventoryPrice = medicine.getSellingPrice();
            BigDecimal requestedPrice = itemRequest.pricePerUnit();
            BigDecimal pricePerUnit;
            if (allowOverride) {
                if (requestedPrice == null) {
                    throw new ApiException("Price per unit is required when override is enabled");
                }
                pricePerUnit = requestedPrice;
            } else {
                if (requestedPrice != null && requestedPrice.compareTo(inventoryPrice) != 0) {
                    throw new ApiException("Manual price override is not allowed");
                }
                pricePerUnit = inventoryPrice;
            }
            if (pricePerUnit.compareTo(BigDecimal.ZERO) < 0) {
                throw new ApiException("Price per unit cannot be negative");
            }

            String dosageInstruction = trimToNull(itemRequest.dosageInstruction());
            if (dosageInstruction == null) {
                throw new ApiException("Dosage instruction is required");
            }
            String customDosageInstruction = trimToNull(itemRequest.customDosageInstruction());
            if ("CUSTOM".equalsIgnoreCase(dosageInstruction) && customDosageInstruction == null) {
                throw new ApiException("Custom dosage instruction is required when CUSTOM is selected");
            }

            medicine.setQuantity(medicine.getQuantity() - itemRequest.quantity());
            medicineRepository.save(medicine);

            BigDecimal quantity = BigDecimal.valueOf(itemRequest.quantity());
            BigDecimal lineTotal = pricePerUnit.multiply(quantity);
            BigDecimal lineCost = medicine.getPurchasePrice().multiply(quantity);
            beforeDiscount = beforeDiscount.add(lineTotal);

            SaleItem saleItem = new SaleItem();
            saleItem.setSale(sale);
            saleItem.setMedicine(medicine);
            saleItem.setMedicineNameSnapshot(
                    trimToNull(itemRequest.medicineName()) == null ? medicine.getName() : itemRequest.medicineName().trim()
            );
            saleItem.setQuantity(itemRequest.quantity());
            saleItem.setUnitType(itemRequest.unitType().trim());
            saleItem.setDosageInstruction(dosageInstruction);
            saleItem.setCustomDosageInstruction(customDosageInstruction);
            saleItem.setRemark(trimToNull(itemRequest.remark()));
            saleItem.setUnitPrice(pricePerUnit);
            saleItem.setUnitCost(medicine.getPurchasePrice());
            saleItem.setLineTotal(lineTotal);
            saleItem.setLineCost(lineCost);
            sale.getItems().add(saleItem);
        }

        BigDecimal discount = request.discountAmount() == null ? BigDecimal.ZERO : request.discountAmount();
        if (discount.compareTo(BigDecimal.ZERO) < 0) {
            throw new ApiException("Discount cannot be negative");
        }
        if (discount.compareTo(beforeDiscount) > 0) {
            throw new ApiException("Discount cannot exceed total");
        }

        sale.setTotalBeforeDiscount(beforeDiscount);
        sale.setDiscountAmount(discount);
        sale.setTotalAfterDiscount(beforeDiscount.subtract(discount));

        Sale saved = saleRepository.save(sale);
        return toBill(saved);
    }

    @Transactional(readOnly = true)
    public List<SaleTransactionSummaryResponse> findTransactions(String transactionId, String salesPerson, LocalDate fromDate, LocalDate toDate) {
        Long tenantId = currentUserService.getCurrentTenantId();
        Long pharmacyId = currentUserService.getCurrentPharmacy().getId();
        Instant start = fromDate == null
                ? Instant.EPOCH
                : fromDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end = toDate == null
                ? Instant.now().plusSeconds(1)
                : toDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        String tx = trimToNull(transactionId);
        String username = trimToNull(salesPerson);
        List<Sale> sales;
        if (tx == null && username == null) {
            sales = saleRepository.findByTenant_IdAndPharmacy_IdAndCreatedAtBetweenOrderByCreatedAtDesc(tenantId, pharmacyId, start, end);
        } else if (tx == null) {
            sales = saleRepository.findByTenant_IdAndPharmacy_IdAndCreatedAtBetweenAndCreatedBy_UsernameContainingIgnoreCaseOrderByCreatedAtDesc(tenantId, pharmacyId, start, end, username);
        } else if (username == null) {
            sales = saleRepository.findByTenant_IdAndPharmacy_IdAndTransactionIdContainingIgnoreCaseAndCreatedAtBetweenOrderByCreatedAtDesc(tenantId, pharmacyId, tx, start, end);
        } else {
            sales = saleRepository
                    .findByTenant_IdAndPharmacy_IdAndTransactionIdContainingIgnoreCaseAndCreatedAtBetweenAndCreatedBy_UsernameContainingIgnoreCaseOrderByCreatedAtDesc(
                            tenantId,
                            pharmacyId,
                            tx,
                            start,
                            end,
                            username
                    );
        }

        return sales.stream().map(this::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public Page<SaleTransactionSummaryResponse> findTransactions(String transactionId, String salesPerson, LocalDate fromDate, LocalDate toDate, int page, int size) {
        Long tenantId = currentUserService.getCurrentTenantId();
        Long pharmacyId = currentUserService.getCurrentPharmacy().getId();
        Instant start = fromDate == null
                ? Instant.EPOCH
                : fromDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end = toDate == null
                ? Instant.now().plusSeconds(1)
                : toDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        String tx = trimToNull(transactionId);
        String username = trimToNull(salesPerson);
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        Page<Sale> sales;
        if (tx == null && username == null) {
            sales = saleRepository.findByTenant_IdAndPharmacy_IdAndCreatedAtBetweenOrderByCreatedAtDesc(tenantId, pharmacyId, start, end, pageRequest);
        } else if (tx == null) {
            sales = saleRepository.findByTenant_IdAndPharmacy_IdAndCreatedAtBetweenAndCreatedBy_UsernameContainingIgnoreCaseOrderByCreatedAtDesc(tenantId, pharmacyId, start, end, username, pageRequest);
        } else if (username == null) {
            sales = saleRepository.findByTenant_IdAndPharmacy_IdAndTransactionIdContainingIgnoreCaseAndCreatedAtBetweenOrderByCreatedAtDesc(tenantId, pharmacyId, tx, start, end, pageRequest);
        } else {
            sales = saleRepository
                    .findByTenant_IdAndPharmacy_IdAndTransactionIdContainingIgnoreCaseAndCreatedAtBetweenAndCreatedBy_UsernameContainingIgnoreCaseOrderByCreatedAtDesc(
                            tenantId,
                            pharmacyId,
                            tx,
                            start,
                            end,
                            username,
                            pageRequest
                    );
        }

        return sales.map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public List<BillingMedicineOptionResponse> listBillingMedicines() {
        Long tenantId = currentUserService.getCurrentTenantId();
        Long pharmacyId = currentUserService.getCurrentPharmacy().getId();
        return medicineRepository.findByTenant_IdAndPharmacy_Id(tenantId, pharmacyId).stream()
                .map(medicine -> new BillingMedicineOptionResponse(
                        medicine.getId(),
                        medicine.getName(),
                        medicine.getUnitType(),
                        medicine.getSellingPrice(),
                        medicine.getQuantity(),
                        medicine.getQuantity() > 0
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public SaleBillResponse getBillByTransactionId(String transactionId) {
        Long tenantId = currentUserService.getCurrentTenantId();
        Long pharmacyId = currentUserService.getCurrentPharmacy().getId();
        Sale sale = saleRepository.findByTransactionIdAndTenant_IdAndPharmacy_Id(transactionId, tenantId, pharmacyId)
                .orElseThrow(() -> new ApiException("Transaction not found"));
        return toBill(sale);
    }

    @Transactional(readOnly = true)
    public SalesSummaryResponse getSalesSummary(SalesPeriod period) {
        Long tenantId = currentUserService.getCurrentTenantId();
        Long pharmacyId = currentUserService.getCurrentPharmacy().getId();
        ZoneId zone = ZoneId.systemDefault();
        LocalDate now = LocalDate.now(zone);

        LocalDate startDate;
        LocalDate endExclusiveDate;

        switch (period) {
            case DAY -> {
                startDate = now;
                endExclusiveDate = now.plusDays(1);
            }
            case WEEK -> {
                startDate = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                endExclusiveDate = startDate.plusDays(7);
            }
            case MONTH -> {
                startDate = now.withDayOfMonth(1);
                endExclusiveDate = startDate.plusMonths(1);
            }
            case YEAR -> {
                startDate = now.withDayOfYear(1);
                endExclusiveDate = startDate.plusYears(1);
            }
            default -> throw new ApiException("Unsupported period");
        }

        Instant start = startDate.atStartOfDay(zone).toInstant();
        Instant end = endExclusiveDate.atStartOfDay(zone).toInstant();

        BigDecimal totalSales = saleRepository.sumTotalBetween(tenantId, pharmacyId, start, end);
        BigDecimal totalCost = saleItemRepository.sumCostBetween(tenantId, pharmacyId, start, end);
        long saleCount = saleRepository.countByTenant_IdAndPharmacy_IdAndCreatedAtBetween(tenantId, pharmacyId, start, end);

        return new SalesSummaryResponse(
                period,
                startDate.toString(),
                endExclusiveDate.minusDays(1).toString(),
                saleCount,
                totalSales,
                totalCost,
                totalSales.subtract(totalCost),
                saleItemRepository.findTopSellingBetween(tenantId, pharmacyId, start, end).stream()
                        .limit(5)
                        .map(row -> new TopMedicineSales(
                                String.valueOf(row[0]),
                                ((Number) row[1]).longValue()
                        ))
                        .toList(),
                saleRepository.summarizeSalesByUser(tenantId, pharmacyId, start, end).stream()
                        .map(row -> new UserSalesSummary(
                                String.valueOf(row[0]),
                                ((Number) row[1]).longValue(),
                                (BigDecimal) row[2]
                        ))
                        .toList()
        );
    }

    private SaleBillResponse toBill(Sale sale) {
        return new SaleBillResponse(
                sale.getTransactionId(),
                sale.getCreatedAt(),
                sale.getCreatedBy().getUsername(),
                sale.getCustomerName(),
                sale.getCustomerPhone(),
                sale.getItems().stream().map(item -> new SaleBillItemResponse(
                        item.getMedicineNameSnapshot(),
                        item.getQuantity(),
                        item.getUnitType(),
                        item.getDosageInstruction(),
                        item.getCustomDosageInstruction(),
                        item.getRemark(),
                        item.getUnitPrice(),
                        item.getLineTotal()
                )).toList(),
                sale.getTotalBeforeDiscount(),
                sale.getDiscountAmount(),
                sale.getTotalAfterDiscount()
        );
    }

    private SaleTransactionSummaryResponse toSummary(Sale sale) {
        return new SaleTransactionSummaryResponse(
                sale.getTransactionId(),
                sale.getCreatedAt(),
                sale.getCreatedBy().getUsername(),
                sale.getCustomerName(),
                sale.getItems().stream()
                        .map(SaleItem::getMedicineNameSnapshot)
                        .distinct()
                        .toList(),
                sale.getTotalAfterDiscount(),
                sale.getItems().size()
        );
    }

    private String generateTransactionId() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "TXN-" + datePart + "-" + suffix;
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}

