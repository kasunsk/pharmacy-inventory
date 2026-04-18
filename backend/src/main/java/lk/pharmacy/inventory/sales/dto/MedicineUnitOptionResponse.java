package lk.pharmacy.inventory.sales.dto;

import java.math.BigDecimal;

public record MedicineUnitOptionResponse(
        String unitType,
        String parentUnit,
        Integer unitsPerParent,
        int conversionToBase,
        BigDecimal purchasePrice,
        BigDecimal sellingPrice,
        int availableQuantity
) {
}

