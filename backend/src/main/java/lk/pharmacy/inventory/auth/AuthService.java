package lk.pharmacy.inventory.auth;

import jakarta.persistence.EntityManager;
import lk.pharmacy.inventory.auth.dto.LoginResponse;
import lk.pharmacy.inventory.domain.Pharmacy;
import lk.pharmacy.inventory.domain.Role;
import lk.pharmacy.inventory.domain.Tenant;
import lk.pharmacy.inventory.domain.User;
import lk.pharmacy.inventory.exception.ApiException;
import lk.pharmacy.inventory.repo.TenantRepository;
import lk.pharmacy.inventory.repo.UserRepository;
import lk.pharmacy.inventory.security.JwtService;
import lk.pharmacy.inventory.util.CurrentUserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class AuthService {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUserService;
    private final EntityManager entityManager;

    public AuthService(JwtService jwtService,
                       UserRepository userRepository,
                       TenantRepository tenantRepository,
                       PasswordEncoder passwordEncoder,
                       CurrentUserService currentUserService,
                       EntityManager entityManager) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentUserService = currentUserService;
        this.entityManager = entityManager;
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

        User user = userRepository.findByUsernameAndTenant_CodeIgnoreCaseWithPharmacies(actualUsername, tenantCode)
                .orElseThrow(() -> new ApiException("Invalid username or password"));

        if (!user.isEnabled()) {
            throw new ApiException("User account is disabled");
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ApiException("Invalid username or password");
        }

        List<Pharmacy> accessiblePharmacies = resolveAccessiblePharmacies(user, true);
        if (accessiblePharmacies.isEmpty()) {
            throw new ApiException("No active pharmacy assigned to this user");
        }

        Pharmacy selectedPharmacy = resolveSelectedPharmacy(user, tenant, accessiblePharmacies);
        boolean requiresSelection = accessiblePharmacies.size() > 1 && selectedPharmacy == null;
        Long selectedPharmacyId = selectedPharmacy == null ? null : selectedPharmacy.getId();

        String token = jwtService.generateToken(user.getUsername(), user.getTenant().getId(), selectedPharmacyId);
        boolean legacyUnconfigured = isLegacyUnconfigured(tenant);
        return new LoginResponse(token,
                user.getUsername(),
                user.getRoles(),
                user.getTenant().getId(),
                user.getTenant().getCode(),
                user.getTenant().getName(),
                tenant.getLogoData() != null && tenant.getLogoData().length > 0,
                selectedPharmacyId,
                selectedPharmacy == null ? null : selectedPharmacy.getName(),
                requiresSelection,
                accessiblePharmacies.stream()
                        .map(pharmacy -> new LoginResponse.PharmacySummary(
                                pharmacy.getId(),
                                pharmacy.getCode(),
                                pharmacy.getName(),
                                pharmacy.isEnabled(),
                                pharmacy.getLogoData() != null && pharmacy.getLogoData().length > 0
                        ))
                        .toList(),
                legacyUnconfigured || tenant.isBillingEnabled(),
                legacyUnconfigured || tenant.isTransactionsEnabled(),
                legacyUnconfigured || tenant.isInventoryEnabled(),
                legacyUnconfigured || tenant.isAnalyticsEnabled(),
                legacyUnconfigured || tenant.isAiAssistantEnabled());
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

        String token = jwtService.generateToken(user.getUsername(), null, null);
        return new LoginResponse(token, user.getUsername(), user.getRoles(), null, null, null, false, null, null, false, List.of(), false, false, false, false, false);
    }

    public LoginResponse switchPharmacy(Long pharmacyId) {
        User user = currentUserService.getCurrentTenantUser();
        if (pharmacyId == null) {
            throw new ApiException("pharmacyId is required");
        }
        List<Pharmacy> accessiblePharmacies = resolveAccessiblePharmacies(user, true);
        Pharmacy selected = accessiblePharmacies.stream()
                .filter(pharmacy -> pharmacy.getId().equals(pharmacyId))
                .findFirst()
                .orElseThrow(() -> new ApiException("You do not have access to this pharmacy"));

        Tenant tenant = user.getTenant();
        String token = jwtService.generateToken(user.getUsername(), tenant.getId(), selected.getId());
        boolean legacyUnconfigured = isLegacyUnconfigured(tenant);
        return new LoginResponse(
                token,
                user.getUsername(),
                user.getRoles(),
                tenant.getId(),
                tenant.getCode(),
                tenant.getName(),
                tenant.getLogoData() != null && tenant.getLogoData().length > 0,
                selected.getId(),
                selected.getName(),
                false,
                accessiblePharmacies.stream().map(ph -> new LoginResponse.PharmacySummary(
                        ph.getId(),
                        ph.getCode(),
                        ph.getName(),
                        ph.isEnabled(),
                        ph.getLogoData() != null && ph.getLogoData().length > 0
                )).toList(),
                legacyUnconfigured || tenant.isBillingEnabled(),
                legacyUnconfigured || tenant.isTransactionsEnabled(),
                legacyUnconfigured || tenant.isInventoryEnabled(),
                legacyUnconfigured || tenant.isAnalyticsEnabled(),
                legacyUnconfigured || tenant.isAiAssistantEnabled()
        );
    }

    public LoginResponse setMyDefaultPharmacy(Long pharmacyId) {
        User user = currentUserService.getCurrentTenantUser();
        List<Pharmacy> accessiblePharmacies = resolveAccessiblePharmacies(user, true);
        Pharmacy selected = accessiblePharmacies.stream()
                .filter(pharmacy -> pharmacy.getId().equals(pharmacyId))
                .findFirst()
                .orElseThrow(() -> new ApiException("You do not have access to this pharmacy"));

        user.setDefaultPharmacy(selected);
        userRepository.save(user);
        return switchPharmacy(selected.getId());
    }

    private Pharmacy resolveSelectedPharmacy(User user, Tenant tenant, List<Pharmacy> accessiblePharmacies) {
        if (accessiblePharmacies.size() == 1) {
            return accessiblePharmacies.get(0);
        }
        if (user.getDefaultPharmacy() != null) {
            Long id = user.getDefaultPharmacy().getId();
            return accessiblePharmacies.stream().filter(pharmacy -> pharmacy.getId().equals(id)).findFirst().orElse(null);
        }
        if (tenant.getDefaultPharmacy() != null) {
            Long id = tenant.getDefaultPharmacy().getId();
            return accessiblePharmacies.stream().filter(pharmacy -> pharmacy.getId().equals(id)).findFirst().orElse(null);
        }
        return null;
    }

    private List<Pharmacy> resolveAccessiblePharmacies(User user, boolean onlyEnabled) {
        if (user.hasRole(Role.ADMIN)) {
            String query = onlyEnabled
                    ? "select p from Pharmacy p where p.tenant.id = :tenantId and p.enabled = true order by p.name"
                    : "select p from Pharmacy p where p.tenant.id = :tenantId order by p.name";
            return entityManager.createQuery(query, Pharmacy.class)
                    .setParameter("tenantId", user.getTenant().getId())
                    .getResultList();
        }
        return user.getAssignedPharmacies().stream()
                .filter(pharmacy -> !onlyEnabled || pharmacy.isEnabled())
                .sorted(Comparator.comparing(Pharmacy::getName))
                .toList();
    }

    private boolean isLegacyUnconfigured(Tenant tenant) {
        return !tenant.isBillingEnabled()
                && !tenant.isTransactionsEnabled()
                && !tenant.isInventoryEnabled()
                && !tenant.isAnalyticsEnabled()
                && !tenant.isAiAssistantEnabled();
    }
}
