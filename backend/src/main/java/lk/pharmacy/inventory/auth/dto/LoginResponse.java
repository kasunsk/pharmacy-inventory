package lk.pharmacy.inventory.auth.dto;

import lk.pharmacy.inventory.domain.Role;

public record LoginResponse(
        String token,
        String username,
        Role role
) {
}

