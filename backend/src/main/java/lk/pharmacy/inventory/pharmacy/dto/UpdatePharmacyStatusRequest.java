package lk.pharmacy.inventory.pharmacy.dto;

import jakarta.validation.constraints.NotNull;

public record UpdatePharmacyStatusRequest(@NotNull Boolean enabled) {
}

