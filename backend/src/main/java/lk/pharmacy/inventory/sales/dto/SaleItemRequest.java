package lk.pharmacy.inventory.sales.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SaleItemRequest(
        @NotNull Long medicineId,
        @Min(1) int quantity
) {
}

