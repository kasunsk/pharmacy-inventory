package lk.pharmacy.inventory.sales.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SaleItemRequest(
        @NotNull Long medicineId,
        String medicineName,
        @Min(1) int quantity,
        @NotBlank String unitType,
        BigDecimal pricePerUnit
) {
}

