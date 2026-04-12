package lk.pharmacy.inventory.employee;
import lk.pharmacy.inventory.domain.Role;
import lk.pharmacy.inventory.domain.User;
import lk.pharmacy.inventory.employee.dto.CreateEmployeeRequest;
import lk.pharmacy.inventory.employee.dto.EmployeeResponse;
import lk.pharmacy.inventory.employee.dto.UpdateEmployeeRequest;
import lk.pharmacy.inventory.exception.ApiException;
import lk.pharmacy.inventory.repo.UserRepository;
import lk.pharmacy.inventory.util.CurrentUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Set;
@Service
public class EmployeeService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUserService;
    public EmployeeService(UserRepository userRepository, PasswordEncoder passwordEncoder, CurrentUserService currentUserService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentUserService = currentUserService;
    }
    @Transactional(readOnly = true)
    public List<EmployeeResponse> list() {
        Long tenantId = currentUserService.getCurrentUser().getTenant().getId();
        return userRepository.findByTenant_Id(tenantId).stream()
                .map(this::toResponse)
                .toList();
    }
    @Transactional(readOnly = true)
    public Page<EmployeeResponse> list(int page, int size) {
        Long tenantId = currentUserService.getCurrentUser().getTenant().getId();
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("username").ascending());
        return userRepository.findByTenant_Id(tenantId, pageRequest).map(this::toResponse);
    }
    @Transactional
    public EmployeeResponse create(CreateEmployeeRequest request) {
        User currentUser = currentUserService.getCurrentUser();
        Long tenantId = currentUser.getTenant().getId();
        if (userRepository.existsByUsernameAndTenant_Id(request.username(), tenantId)) {
            throw new ApiException("Username already exists");
        }
        Set<Role> roles = sanitizeRoles(request.roles(), Set.of(Role.BILLING));
        User user = new User();
        user.setTenant(currentUser.getTenant());
        user.setUsername(request.username().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRoles(roles);
        applyProfilePatch(user, request.firstName(), request.lastName(), request.phoneNumber(), request.email(), request.address(), request.birthdate(), request.gender());
        return toResponse(userRepository.save(user));
    }
    @Transactional
    public EmployeeResponse update(Long id, UpdateEmployeeRequest request) {
        Long tenantId = currentUserService.getCurrentUser().getTenant().getId();
        User user = userRepository.findByIdAndTenant_Id(id, tenantId)
                .orElseThrow(() -> new ApiException("Employee not found"));
        if (request.username() != null && !request.username().trim().isEmpty()) {
            String nextUsername = request.username().trim();
            userRepository.findByUsernameAndTenant_Id(nextUsername, tenantId)
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
        applyProfile(user, request.firstName(), request.lastName(), request.phoneNumber(), request.email(), request.address(), request.birthdate(), request.gender());
        return toResponse(userRepository.save(user));
    }
    @Transactional
    public void delete(Long id) {
        Long tenantId = currentUserService.getCurrentUser().getTenant().getId();
        User user = userRepository.findByIdAndTenant_Id(id, tenantId)
                .orElseThrow(() -> new ApiException("Employee not found"));
        userRepository.delete(user);
    }
    @Transactional(readOnly = true)
    public EmployeeResponse profile() {
        return toResponse(currentUserService.getCurrentUser());
    }
    @Transactional
    public EmployeeResponse updateProfile(UpdateEmployeeRequest request) {
        User user = currentUserService.getCurrentUser();
        if (request.password() != null && !request.password().trim().isEmpty()) {
            user.setPasswordHash(passwordEncoder.encode(request.password().trim()));
        }
        applyProfilePatch(user, request.firstName(), request.lastName(), request.phoneNumber(), request.email(), request.address(), request.birthdate(), request.gender());
        return toResponse(userRepository.save(user));
    }
    private Set<Role> sanitizeRoles(Set<Role> roles, Set<Role> fallback) {
        Set<Role> resolved = (roles == null || roles.isEmpty()) ? fallback : roles;
        if (resolved == null || resolved.isEmpty()) {
            throw new ApiException("At least one role is required");
        }
        return resolved;
    }
    private void applyProfile(User user, String firstName, String lastName, String phoneNumber, String email, String address, String birthdate, String gender) {
        user.setFirstName(trimToNull(firstName));
        user.setLastName(trimToNull(lastName));
        user.setPhoneNumber(trimToNull(phoneNumber));
        user.setEmail(trimToNull(email));
        user.setAddress(trimToNull(address));
        user.setBirthdate(trimToNull(birthdate));
        user.setGender(trimToNull(gender));
    }
    private void applyProfilePatch(User user, String firstName, String lastName, String phoneNumber, String email, String address, String birthdate, String gender) {
        if (firstName != null) user.setFirstName(trimToNull(firstName));
        if (lastName != null) user.setLastName(trimToNull(lastName));
        if (phoneNumber != null) user.setPhoneNumber(trimToNull(phoneNumber));
        if (email != null) user.setEmail(trimToNull(email));
        if (address != null) user.setAddress(trimToNull(address));
        if (birthdate != null) user.setBirthdate(trimToNull(birthdate));
        if (gender != null) user.setGender(trimToNull(gender));
    }
    private String trimToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
    private EmployeeResponse toResponse(User user) {
        return new EmployeeResponse(
                user.getId(),
                user.getUsername(),
                user.getRoles(),
                user.isEnabled(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhoneNumber(),
                user.getEmail(),
                user.getAddress(),
                user.getBirthdate(),
                user.getGender()
        );
    }
}
