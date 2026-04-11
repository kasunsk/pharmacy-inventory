package lk.pharmacy.inventory.sales.dto;

import java.math.BigDecimal;

public record SaleBillItemResponse(
        String medicineName,
        int quantity,
        String unitType,
        String dosageInstruction,
        String customDosageInstruction,
        String remark,
        BigDecimal pricePerUnit,
        BigDecimal lineTotal
) {
}

