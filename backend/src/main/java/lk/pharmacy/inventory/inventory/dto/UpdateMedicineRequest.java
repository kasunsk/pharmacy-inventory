package lk.pharmacy.inventory.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record UpdateMedicineRequest(
        @NotBlank String name,
        @NotBlank String batchNumber,
        @NotNull LocalDate expiryDate,
        @NotBlank String supplier,
        @NotBlank String unitType,
        List<String> allowedUnits,
        @NotNull BigDecimal purchasePrice,
        @NotNull BigDecimal sellingPrice,
        @Min(0) int quantity,
        @NotBlank String modificationReason
) {
    public MedicineRequest toMedicineRequest() {
        return new MedicineRequest(
                name,
                batchNumber,
                expiryDate,
                supplier,
                unitType,
                allowedUnits,
                purchasePrice,
                sellingPrice,
                quantity
        );
    }
}
