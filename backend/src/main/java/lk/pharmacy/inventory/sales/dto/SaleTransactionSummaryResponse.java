package lk.pharmacy.inventory.sales.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record SaleTransactionSummaryResponse(
        String transactionId,
        Instant dateTime,
        String customerName,
        BigDecimal totalAmount,
        int itemCount
) {
}

