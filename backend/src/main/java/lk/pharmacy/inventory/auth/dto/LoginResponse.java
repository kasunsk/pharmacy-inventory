package lk.pharmacy.inventory.auth.dto;

import lk.pharmacy.inventory.domain.Role;

import java.util.Set;

public record LoginResponse(
        String token,
        String username,
        Set<Role> roles,
        Long tenantId,
        String tenantCode,
        String tenantName,
        boolean billingEnabled,
        boolean transactionsEnabled,
        boolean inventoryEnabled,
        boolean analyticsEnabled,
        boolean aiAssistantEnabled
) {
}

