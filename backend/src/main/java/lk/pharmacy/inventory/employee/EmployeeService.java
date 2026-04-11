package lk.pharmacy.inventory.employee;
import lk.pharmacy.inventory.domain.Role;
import lk.pharmacy.inventory.domain.User;
import lk.pharmacy.inventory.employee.dto.CreateEmployeeRequest;
import lk.pharmacy.inventory.employee.dto.EmployeeResponse;
import lk.pharmacy.inventory.employee.dto.UpdateEmployeeRequest;
import lk.pharmacy.inventory.exception.ApiException;
import lk.pharmacy.inventory.repo.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Set;
@Service
public class EmployeeService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    public EmployeeService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    @Transactional(readOnly = true)
    public List<EmployeeResponse> list() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }
    @Transactional
    public EmployeeResponse create(CreateEmployeeRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ApiException("Username already exists");
        }
        Set<Role> roles = sanitizeRoles(request.roles(), Set.of(Role.BILLING));
        User user = new User();
        user.setUsername(request.username().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRoles(roles);
        return toResponse(userRepository.save(user));
    }
    @Transactional
    public EmployeeResponse update(Long id, UpdateEmployeeRequest request) {
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
        return toResponse(userRepository.save(user));
    }
    @Transactional
    public void delete(Long id) {
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
    private EmployeeResponse toResponse(User user) {
        return new EmployeeResponse(
                user.getId(),
                user.getUsername(),
                user.getRoles(),
                user.isEnabled()
        );
    }
}
