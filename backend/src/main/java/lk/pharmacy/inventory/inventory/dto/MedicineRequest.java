package lk.pharmacy.inventory.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MedicineRequest(
        @NotBlank String name,
        @NotBlank String batchNumber,
        @NotNull LocalDate expiryDate,
        @NotBlank String supplier,
        @NotNull BigDecimal purchasePrice,
        @NotNull BigDecimal sellingPrice,
        @Min(0) int quantity
) {
}

