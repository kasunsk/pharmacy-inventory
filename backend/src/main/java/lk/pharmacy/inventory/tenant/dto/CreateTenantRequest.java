package lk.pharmacy.inventory.tenant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateTenantRequest(
        @NotBlank @Size(max = 80) String code,
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Size(max = 100) String adminUsername,
        @NotBlank @Size(min = 6, max = 120) String adminPassword,
        @NotBlank @Pattern(regexp = "MALE|FEMALE", message = "gender must be MALE or FEMALE") String adminGender
) {
}
