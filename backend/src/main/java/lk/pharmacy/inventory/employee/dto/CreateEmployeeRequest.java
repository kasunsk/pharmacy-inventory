package lk.pharmacy.inventory.employee.dto;

import jakarta.validation.constraints.NotBlank;
import lk.pharmacy.inventory.domain.Role;

import java.util.Set;

public record CreateEmployeeRequest(
        @NotBlank String username,
        @NotBlank String password,
        Set<Role> roles,
        String firstName,
        String lastName,
        String phoneNumber,
        String email,
        String address,
        String birthdate,
        String gender
) {
}

