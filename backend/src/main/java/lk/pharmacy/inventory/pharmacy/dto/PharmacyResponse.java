package lk.pharmacy.inventory.pharmacy.dto;

public record PharmacyResponse(
        Long id,
        Long tenantId,
        String code,
        String name,
        boolean enabled,
        boolean hasLogo
) {
}

