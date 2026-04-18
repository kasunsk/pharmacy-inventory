package lk.pharmacy.inventory.sales;

import lk.pharmacy.inventory.domain.Medicine;
import lk.pharmacy.inventory.domain.MedicineUnitDefinition;
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
import java.util.*;

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

            String requestedUnit = normalizeUnit(itemRequest.unitType());
            Map<String, MedicineUnitDefinition> unitDefinitions = resolveUnitDefinitions(medicine);
            MedicineUnitDefinition selectedUnit = unitDefinitions.get(requestedUnit);
            if (selectedUnit == null) {
                throw new ApiException("Invalid unit for medicine: " + medicine.getName());
            }

            long requiredBaseQuantityLong = (long) itemRequest.quantity() * selectedUnit.getConversionToBase();
            if (requiredBaseQuantityLong > Integer.MAX_VALUE) {
                throw new ApiException("Requested quantity is too large");
            }
            int requiredBaseQuantity = (int) requiredBaseQuantityLong;

            int currentBaseQuantity = effectiveBaseQuantity(medicine);
            if (currentBaseQuantity < requiredBaseQuantity) {
                throw new ApiException("Insufficient stock for medicine: " + medicine.getName());
            }

            boolean allowOverride = Boolean.TRUE.equals(itemRequest.allowPriceOverride());
            BigDecimal inventoryPrice = selectedUnit.getSellingPrice();
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

            medicine.setBaseQuantity(currentBaseQuantity - requiredBaseQuantity);
            medicine.setQuantity(medicine.getBaseQuantity()); // legacy compatibility
            medicineRepository.save(medicine);

            BigDecimal quantity = BigDecimal.valueOf(itemRequest.quantity());
            BigDecimal lineTotal = pricePerUnit.multiply(quantity);
            BigDecimal lineCost = selectedUnit.getPurchasePrice().multiply(quantity);
            beforeDiscount = beforeDiscount.add(lineTotal);

            SaleItem saleItem = new SaleItem();
            saleItem.setSale(sale);
            saleItem.setMedicine(medicine);
            saleItem.setMedicineNameSnapshot(
                    trimToNull(itemRequest.medicineName()) == null ? medicine.getName() : itemRequest.medicineName().trim()
            );
            saleItem.setQuantity(itemRequest.quantity());
            saleItem.setUnitType(requestedUnit);
            saleItem.setDosageInstruction(dosageInstruction);
            saleItem.setCustomDosageInstruction(customDosageInstruction);
            saleItem.setRemark(trimToNull(itemRequest.remark()));
            saleItem.setUnitPrice(pricePerUnit);
            saleItem.setUnitCost(selectedUnit.getPurchasePrice());
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
    public List<SaleTransactionSummaryResponse> findTransactions(String transactionId, LocalDate fromDate, LocalDate toDate) {
        Long tenantId = currentUserService.getCurrentTenantId();
        Long pharmacyId = currentUserService.getCurrentPharmacy().getId();
        Instant start = fromDate == null
                ? Instant.EPOCH
                : fromDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end = toDate == null
                ? Instant.now().plusSeconds(1)
                : toDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        String tx = trimToNull(transactionId);
        List<Sale> sales;
        if (tx == null) {
            sales = saleRepository.findByTenant_IdAndPharmacy_IdAndCreatedAtBetweenOrderByCreatedAtDesc(tenantId, pharmacyId, start, end);
        } else {
            sales = saleRepository.findByTenant_IdAndPharmacy_IdAndTransactionIdContainingIgnoreCaseAndCreatedAtBetweenOrderByCreatedAtDesc(tenantId, pharmacyId, tx, start, end);
        }

        return sales.stream().map(this::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public Page<SaleTransactionSummaryResponse> findTransactions(String transactionId, LocalDate fromDate, LocalDate toDate, int page, int size) {
        Long tenantId = currentUserService.getCurrentTenantId();
        Long pharmacyId = currentUserService.getCurrentPharmacy().getId();
        Instant start = fromDate == null
                ? Instant.EPOCH
                : fromDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end = toDate == null
                ? Instant.now().plusSeconds(1)
                : toDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        String tx = trimToNull(transactionId);
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        Page<Sale> sales;
        if (tx == null) {
            sales = saleRepository.findByTenant_IdAndPharmacy_IdAndCreatedAtBetweenOrderByCreatedAtDesc(tenantId, pharmacyId, start, end, pageRequest);
        } else {
            sales = saleRepository.findByTenant_IdAndPharmacy_IdAndTransactionIdContainingIgnoreCaseAndCreatedAtBetweenOrderByCreatedAtDesc(tenantId, pharmacyId, tx, start, end, pageRequest);
        }

        return sales.map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public List<BillingMedicineOptionResponse> listBillingMedicines() {
        Long tenantId = currentUserService.getCurrentTenantId();
        Long pharmacyId = currentUserService.getCurrentPharmacy().getId();
        return medicineRepository.findByTenant_IdAndPharmacy_Id(tenantId, pharmacyId).stream()
                .map(medicine -> {
                    Map<String, MedicineUnitDefinition> definitions = resolveUnitDefinitions(medicine);
                    List<MedicineUnitOptionResponse> unitOptions = definitions.values().stream()
                            .sorted(Comparator.comparingInt(MedicineUnitDefinition::getConversionToBase))
                            .map(definition -> new MedicineUnitOptionResponse(
                                    definition.getUnitType(),
                                    definition.getParentUnit(),
                                    definition.getUnitsPerParent(),
                                    definition.getConversionToBase(),
                                    definition.getPurchasePrice(),
                                    definition.getSellingPrice(),
                                    definition.getConversionToBase() <= 0
                                            ? 0
                                            : effectiveBaseQuantity(medicine) / definition.getConversionToBase()
                            ))
                            .toList();

                    int baseQuantity = effectiveBaseQuantity(medicine);
                    return new BillingMedicineOptionResponse(
                            medicine.getId(),
                            medicine.getName(),
                            medicine.getUnitType(),
                            medicine.getBaseUnit(),
                            resolveAllowedUnits(medicine).stream().toList(),
                            medicine.getSellingPrice(),
                            baseQuantity,
                            baseQuantity > 0,
                            unitOptions
                    );
                })
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

    private String normalizeUnit(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new ApiException("Unit type is required");
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }

    private Set<String> resolveAllowedUnits(Medicine medicine) {
        Set<String> resolved = new LinkedHashSet<>();
        if (medicine.getAllowedUnits() != null) {
            medicine.getAllowedUnits().stream()
                    .map(this::trimToNull)
                    .filter(Objects::nonNull)
                    .map(this::normalizeUnit)
                    .forEach(resolved::add);
        }
        if (resolved.isEmpty() && medicine.getUnitDefinitions() != null) {
            medicine.getUnitDefinitions().stream()
                    .map(MedicineUnitDefinition::getUnitType)
                    .map(this::normalizeUnit)
                    .forEach(resolved::add);
        }
        if (resolved.isEmpty()) {
            resolved.add(normalizeUnit(medicine.getUnitType()));
        }
        return resolved;
    }

    private Map<String, MedicineUnitDefinition> resolveUnitDefinitions(Medicine medicine) {
        LinkedHashMap<String, MedicineUnitDefinition> definitions = new LinkedHashMap<>();
        if (medicine.getUnitDefinitions() != null) {
            for (MedicineUnitDefinition definition : medicine.getUnitDefinitions()) {
                String unit = normalizeUnit(definition.getUnitType());
                definitions.put(unit, definition);
            }
        }

        if (definitions.isEmpty()) {
            for (String unit : resolveAllowedUnits(medicine)) {
                MedicineUnitDefinition fallback = new MedicineUnitDefinition();
                fallback.setUnitType(unit);
                fallback.setParentUnit(null);
                fallback.setUnitsPerParent(null);
                fallback.setConversionToBase(1);
                fallback.setPurchasePrice(medicine.getPurchasePrice());
                fallback.setSellingPrice(medicine.getSellingPrice());
                definitions.put(unit, fallback);
            }
        }
        return definitions;
    }

    private int effectiveBaseQuantity(Medicine medicine) {
        if (medicine.getBaseQuantity() > 0) {
            return medicine.getBaseQuantity();
        }
        return Math.max(medicine.getQuantity(), 0);
    }
}
