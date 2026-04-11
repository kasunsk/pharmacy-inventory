package lk.pharmacy.inventory.sales.dto;

import java.math.BigDecimal;
import java.util.List;

public record SalesSummaryResponse(
        SalesPeriod period,
        String from,
        String to,
        long saleCount,
        BigDecimal totalSales,
        BigDecimal totalCost,
        BigDecimal totalProfit,
        List<TopMedicineSales> topSellingMedicines,
        List<UserSalesSummary> salesByUser
) {
}

