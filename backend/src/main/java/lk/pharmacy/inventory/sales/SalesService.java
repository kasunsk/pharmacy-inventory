package lk.pharmacy.inventory.sales;

import lk.pharmacy.inventory.domain.Medicine;
import lk.pharmacy.inventory.domain.Sale;
import lk.pharmacy.inventory.domain.SaleItem;
import lk.pharmacy.inventory.domain.User;
import lk.pharmacy.inventory.exception.ApiException;
import lk.pharmacy.inventory.repo.MedicineRepository;
import lk.pharmacy.inventory.repo.SaleRepository;
import lk.pharmacy.inventory.sales.dto.CreateSaleRequest;
import lk.pharmacy.inventory.sales.dto.SaleItemRequest;
import lk.pharmacy.inventory.util.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class SalesService {

    private final MedicineRepository medicineRepository;
    private final SaleRepository saleRepository;
    private final CurrentUserService currentUserService;

    public SalesService(MedicineRepository medicineRepository,
                        SaleRepository saleRepository,
                        CurrentUserService currentUserService) {
        this.medicineRepository = medicineRepository;
        this.saleRepository = saleRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public Sale createSale(CreateSaleRequest request) {
        if (request.items() == null || request.items().isEmpty()) {
            throw new ApiException("At least one sale item is required");
        }

        User currentUser = currentUserService.getCurrentUser();
        Sale sale = new Sale();
        sale.setCreatedBy(currentUser);

        BigDecimal beforeDiscount = BigDecimal.ZERO;

        for (SaleItemRequest itemRequest : request.items()) {
            Medicine medicine = medicineRepository.findById(itemRequest.medicineId())
                    .orElseThrow(() -> new ApiException("Medicine not found: " + itemRequest.medicineId()));

            if (medicine.getQuantity() < itemRequest.quantity()) {
                throw new ApiException("Insufficient stock for medicine: " + medicine.getName());
            }

            medicine.setQuantity(medicine.getQuantity() - itemRequest.quantity());
            medicineRepository.save(medicine);

            BigDecimal lineTotal = medicine.getSellingPrice().multiply(BigDecimal.valueOf(itemRequest.quantity()));
            beforeDiscount = beforeDiscount.add(lineTotal);

            SaleItem saleItem = new SaleItem();
            saleItem.setSale(sale);
            saleItem.setMedicine(medicine);
            saleItem.setMedicineNameSnapshot(medicine.getName());
            saleItem.setQuantity(itemRequest.quantity());
            saleItem.setUnitPrice(medicine.getSellingPrice());
            saleItem.setLineTotal(lineTotal);
            sale.getItems().add(saleItem);
        }

        BigDecimal discount = request.discountAmount() == null ? BigDecimal.ZERO : request.discountAmount();
        if (discount.compareTo(beforeDiscount) > 0) {
            throw new ApiException("Discount cannot exceed total");
        }

        sale.setTotalBeforeDiscount(beforeDiscount);
        sale.setDiscountAmount(discount);
        sale.setTotalAfterDiscount(beforeDiscount.subtract(discount));

        return saleRepository.save(sale);
    }
}

