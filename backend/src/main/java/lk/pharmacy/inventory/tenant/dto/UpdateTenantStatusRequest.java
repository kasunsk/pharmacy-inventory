package lk.pharmacy.inventory.tenant.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateTenantStatusRequest(
        @NotNull Boolean enabled
) {
}

