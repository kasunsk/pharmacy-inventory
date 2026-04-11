package lk.pharmacy.inventory.employee.dto;

import lk.pharmacy.inventory.domain.Role;

import java.util.Set;

public record EmployeeResponse(
        Long id,
        String username,
        Set<Role> roles,
        boolean enabled
) {
}

