package lk.pharmacy.inventory.sales.dto;

import java.math.BigDecimal;

public record SalesSummaryResponse(
        SalesPeriod period,
        String from,
        String to,
        long saleCount,
        BigDecimal totalSales,
        BigDecimal totalCost,
        BigDecimal totalProfit
) {
}

