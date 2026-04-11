package lk.pharmacy.inventory.sales.dto;

import java.math.BigDecimal;

public record BillingMedicineOptionResponse(
        Long id,
        String name,
        String unitType,
        BigDecimal sellingPrice,
        int quantity,
        boolean available
) {
}

