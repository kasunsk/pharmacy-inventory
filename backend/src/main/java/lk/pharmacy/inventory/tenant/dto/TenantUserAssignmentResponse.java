package lk.pharmacy.inventory.tenant.dto;
public record TenantUserAssignmentResponse(
        Long userId,
        String username,
        boolean enabled,
        Long tenantId,
        String tenantCode,
        String tenantName
) {
}
