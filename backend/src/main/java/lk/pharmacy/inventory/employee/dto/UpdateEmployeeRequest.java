package lk.pharmacy.inventory.employee.dto;

import lk.pharmacy.inventory.domain.Role;

import java.util.Set;

public record UpdateEmployeeRequest(
        String username,
        String password,
        Set<Role> roles,
        Boolean enabled,
        String firstName,
        String lastName,
        String phoneNumber,
        String email,
        String address,
        String birthdate,
        String gender
) {
}

