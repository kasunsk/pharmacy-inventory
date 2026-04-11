package lk.pharmacy.inventory.sales.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SaleBillResponse(
        String transactionId,
        Instant dateTime,
        String customerName,
        String customerPhone,
        List<SaleBillItemResponse> items,
        BigDecimal totalBeforeDiscount,
        BigDecimal discountAmount,
        BigDecimal totalAmount
) {
}

