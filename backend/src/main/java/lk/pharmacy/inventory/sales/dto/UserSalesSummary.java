package lk.pharmacy.inventory.sales.dto;
import java.math.BigDecimal;
public record UserSalesSummary(
        String username,
        long saleCount,
        BigDecimal totalSales
) {
}
