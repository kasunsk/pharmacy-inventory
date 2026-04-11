package lk.pharmacy.inventory.employee.dto;

import jakarta.validation.constraints.NotBlank;
import lk.pharmacy.inventory.domain.Role;

public record CreateEmployeeRequest(
        @NotBlank String username,
        @NotBlank String password,
        Role role
) {
}

