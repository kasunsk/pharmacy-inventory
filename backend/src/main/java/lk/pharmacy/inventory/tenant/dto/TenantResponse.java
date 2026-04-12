package lk.pharmacy.inventory.tenant.dto;

import java.time.Instant;

public record TenantResponse(
        Long id,
        String code,
        String name,
        boolean enabled,
        boolean billingEnabled,
        boolean transactionsEnabled,
        boolean inventoryEnabled,
        boolean analyticsEnabled,
        boolean aiAssistantEnabled,
        Instant createdAt,
        long userCount
) {
}
