package lk.pharmacy.inventory.tenant.dto;

import java.time.Instant;

public record TenantAuditLogResponse(
        Long tenantId,
        String tenantCode,
        String tenantName,
        String action,
        String performedBy,
        Instant createdAt
) {
}

