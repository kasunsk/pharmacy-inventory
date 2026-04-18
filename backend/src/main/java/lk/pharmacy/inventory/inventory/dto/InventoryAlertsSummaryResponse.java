package lk.pharmacy.inventory.inventory.dto;

import java.time.LocalDate;

public record InventoryAlertsSummaryResponse(
        int lowStockCount,
        int expiringSoonCount,
        int lowStockThreshold,
        int expiryWithinDays,
        LocalDate expiryCutoffDate
) {
}

