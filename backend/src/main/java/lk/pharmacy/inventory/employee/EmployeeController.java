package lk.pharmacy.inventory.employee;

import jakarta.validation.Valid;
import lk.pharmacy.inventory.domain.Role;
import lk.pharmacy.inventory.domain.User;
import lk.pharmacy.inventory.employee.dto.CreateEmployeeRequest;
import lk.pharmacy.inventory.employee.dto.UpdateEmployeeRequest;
import lk.pharmacy.inventory.exception.ApiException;
import lk.pharmacy.inventory.repo.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/employees")
public class EmployeeController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public EmployeeController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<Map<String, Object>> list() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> create(@Valid @RequestBody CreateEmployeeRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ApiException("Username already exists");
        }

        Set<Role> roles = sanitizeRoles(request.roles(), Set.of(Role.BILLING));

        User user = new User();
        user.setUsername(request.username().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRoles(roles);
        userRepository.save(user);

        return toResponse(user);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> update(@PathVariable Long id, @RequestBody UpdateEmployeeRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException("Employee not found"));

        if (request.username() != null && !request.username().trim().isEmpty()) {
            String nextUsername = request.username().trim();
            userRepository.findByUsername(nextUsername)
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(existing -> {
                        throw new ApiException("Username already exists");
                    });
            user.setUsername(nextUsername);
        }

        if (request.password() != null && !request.password().trim().isEmpty()) {
            user.setPasswordHash(passwordEncoder.encode(request.password().trim()));
        }

        if (request.roles() != null) {
            user.setRoles(sanitizeRoles(request.roles(), user.getRoles()));
        }

        if (request.enabled() != null) {
            user.setEnabled(request.enabled());
        }

        userRepository.save(user);
        return toResponse(user);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            throw new ApiException("Employee not found");
        }
        userRepository.deleteById(id);
    }

    private Set<Role> sanitizeRoles(Set<Role> roles, Set<Role> fallback) {
        Set<Role> resolved = (roles == null || roles.isEmpty()) ? fallback : roles;
        if (resolved == null || resolved.isEmpty()) {
            throw new ApiException("At least one role is required");
        }
        return resolved;
    }

    private Map<String, Object> toResponse(User user) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("username", user.getUsername());
        response.put("roles", user.getRoles());
        response.put("enabled", user.isEnabled());
        return response;
    }
}
