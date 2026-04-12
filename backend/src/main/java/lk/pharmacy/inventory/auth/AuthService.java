package lk.pharmacy.inventory.auth;

import lk.pharmacy.inventory.auth.dto.LoginResponse;
import lk.pharmacy.inventory.domain.Role;
import lk.pharmacy.inventory.domain.Tenant;
import lk.pharmacy.inventory.domain.User;
import lk.pharmacy.inventory.exception.ApiException;
import lk.pharmacy.inventory.repo.TenantRepository;
import lk.pharmacy.inventory.repo.UserRepository;
import lk.pharmacy.inventory.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(JwtService jwtService,
                       UserRepository userRepository,
                       TenantRepository tenantRepository,
                       PasswordEncoder passwordEncoder) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResponse login(String usernameInput, String password) {
        String trimmed = usernameInput == null ? "" : usernameInput.trim();

        if ("super_admin".equalsIgnoreCase(trimmed)) {
            return loginSuperAdmin(trimmed, password);
        }

        int atIndex = trimmed.lastIndexOf('@');
        if (atIndex > 0) {
            return loginTenantUser(trimmed, atIndex, password);
        }
        throw new ApiException("Please enter username in format: username@tenant");
    }

    private LoginResponse loginTenantUser(String usernameInput, int atIndex, String password) {
        String actualUsername = usernameInput.substring(0, atIndex).trim();
        String tenantCode = usernameInput.substring(atIndex + 1).trim();

        if (actualUsername.isEmpty() || tenantCode.isEmpty()) {
            throw new ApiException("Please enter username in format: username@tenant");
        }

        Tenant tenant = tenantRepository.findByCodeIgnoreCase(tenantCode)
                .orElseThrow(() -> new ApiException("Tenant is not available. Contact administrator."));

        if (!tenant.isEnabled()) {
            throw new ApiException("Tenant is not available. Contact administrator.");
        }

        User user = userRepository.findByUsernameAndTenant_CodeIgnoreCase(actualUsername, tenantCode)
                .orElseThrow(() -> new ApiException("Invalid username or password"));

        if (!user.isEnabled()) {
            throw new ApiException("User account is disabled");
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ApiException("Invalid username or password");
        }

        String token = jwtService.generateToken(user.getUsername(), user.getTenant().getId());
        boolean legacyUnconfigured = isLegacyUnconfigured(tenant);
        return new LoginResponse(
                token,
                user.getUsername(),
                user.getRoles(),
                user.getTenant().getId(),
                user.getTenant().getCode(),
                user.getTenant().getName(),
                legacyUnconfigured || tenant.isBillingEnabled(),
                legacyUnconfigured || tenant.isTransactionsEnabled(),
                legacyUnconfigured || tenant.isInventoryEnabled(),
                legacyUnconfigured || tenant.isAnalyticsEnabled(),
                legacyUnconfigured || tenant.isAiAssistantEnabled()
        );
    }

    private LoginResponse loginSuperAdmin(String username, String password) {
        User user = userRepository.findByUsernameAndTenantIsNull(username)
                .orElseThrow(() -> new ApiException("Invalid username or password"));

        if (!user.hasRole(Role.SUPER_ADMIN)) {
            throw new ApiException("Invalid username or password");
        }
        if (!user.isEnabled()) {
            throw new ApiException("User account is disabled");
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ApiException("Invalid username or password");
        }

        String token = jwtService.generateToken(user.getUsername(), null);
        return new LoginResponse(token, user.getUsername(), user.getRoles(), null, null, null, false, false, false, false, false);
    }

    private boolean isLegacyUnconfigured(Tenant tenant) {
        return !tenant.isBillingEnabled()
                && !tenant.isTransactionsEnabled()
                && !tenant.isInventoryEnabled()
                && !tenant.isAnalyticsEnabled()
                && !tenant.isAiAssistantEnabled();
    }
}
