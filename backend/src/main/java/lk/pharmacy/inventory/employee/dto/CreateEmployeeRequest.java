package lk.pharmacy.inventory.employee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lk.pharmacy.inventory.domain.Role;

import java.util.Set;

public record CreateEmployeeRequest(
        @NotBlank String username,
        @NotBlank String password,
        Set<Role> roles,
        Set<Long> pharmacyIds,
        Long defaultPharmacyId,
        String firstName,
        String lastName,
        String phoneNumber,
        String email,
        String address,
        String birthdate,
        @NotBlank @Pattern(regexp = "MALE|FEMALE", message = "gender must be MALE or FEMALE") String gender
) {
}

