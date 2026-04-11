package lk.pharmacy.inventory.sales.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record CreateSaleRequest(
        @Valid @NotNull List<SaleItemRequest> items,
        BigDecimal discountAmount
) {
}

