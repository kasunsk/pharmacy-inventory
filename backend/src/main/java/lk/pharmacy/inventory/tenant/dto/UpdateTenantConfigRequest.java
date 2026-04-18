package lk.pharmacy.inventory.tenant.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateTenantConfigRequest(
        @NotNull Boolean billingEnabled,
        @NotNull Boolean transactionsEnabled,
        @NotNull Boolean inventoryEnabled,
        @NotNull Boolean analyticsEnabled,
        @NotNull Boolean aiAssistantEnabled,
        Long defaultPharmacyId
) {
}

