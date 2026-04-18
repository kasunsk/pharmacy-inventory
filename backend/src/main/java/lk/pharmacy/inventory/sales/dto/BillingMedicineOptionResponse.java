package lk.pharmacy.inventory.sales.dto;

import java.math.BigDecimal;
import java.util.List;

public record BillingMedicineOptionResponse(
        Long id,
        String name,
        String unitType,
        String baseUnit,
        List<String> allowedUnits,
        BigDecimal sellingPrice,
        int quantity,
        boolean available,
        List<MedicineUnitOptionResponse> unitOptions
) {
}

