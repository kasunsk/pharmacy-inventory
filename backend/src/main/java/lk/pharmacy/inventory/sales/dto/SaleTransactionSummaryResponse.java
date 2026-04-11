package lk.pharmacy.inventory.sales.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SaleTransactionSummaryResponse(
        String transactionId,
        Instant dateTime,
        String salesPerson,
        String customerName,
        List<String> medicines,
        BigDecimal totalAmount,
        int itemCount
) {
}

