package lk.pharmacy.inventory.tenant;

import lk.pharmacy.inventory.domain.Role;
import lk.pharmacy.inventory.domain.Pharmacy;
import lk.pharmacy.inventory.domain.Tenant;
import lk.pharmacy.inventory.domain.TenantAuditLog;
import lk.pharmacy.inventory.domain.User;
import lk.pharmacy.inventory.exception.ApiException;
import lk.pharmacy.inventory.repo.TenantAuditLogRepository;
import lk.pharmacy.inventory.repo.PharmacyRepository;
import lk.pharmacy.inventory.repo.TenantRepository;
import lk.pharmacy.inventory.repo.UserRepository;
import lk.pharmacy.inventory.tenant.dto.AssignUserTenantRequest;
import lk.pharmacy.inventory.tenant.dto.CreateTenantRequest;
import lk.pharmacy.inventory.tenant.dto.TenantResponse;
import lk.pharmacy.inventory.tenant.dto.TenantAuditLogResponse;
import lk.pharmacy.inventory.tenant.dto.TenantUserAssignmentResponse;
import lk.pharmacy.inventory.tenant.dto.UpdateTenantConfigRequest;
import lk.pharmacy.inventory.tenant.dto.UpdateTenantStatusRequest;
import lk.pharmacy.inventory.util.CurrentUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class TenantService {

    private static final Logger log = LoggerFactory.getLogger(TenantService.class);

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final TenantAuditLogRepository tenantAuditLogRepository;
    private final PharmacyRepository pharmacyRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUserService;

    public TenantService(TenantRepository tenantRepository,
                         UserRepository userRepository,
                         TenantAuditLogRepository tenantAuditLogRepository,
                         PharmacyRepository pharmacyRepository,
                         PasswordEncoder passwordEncoder,
                         CurrentUserService currentUserService) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.tenantAuditLogRepository = tenantAuditLogRepository;
        this.pharmacyRepository = pharmacyRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<TenantResponse> listTenants() {
        return tenantRepository.findAll(Sort.by("name").ascending()).stream()
                .map(this::toTenantResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TenantAuditLogResponse> listAudits(int limit) {
        int resolvedLimit = Math.min(Math.max(limit, 1), 200);
        return tenantAuditLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, resolvedLimit)).stream()
                .map(log -> new TenantAuditLogResponse(
                        log.getTenant().getId(),
                        log.getTenant().getCode(),
                        log.getTenant().getName(),
                        log.getAction(),
                        log.getPerformedBy(),
                        log.getCreatedAt()
                ))
                .toList();
    }

    @Transactional
    public TenantResponse createTenant(CreateTenantRequest request) {
        String code = normalizeCode(request.code());
        String adminUsername = normalizeUsername(request.adminUsername());
        String adminFirstName = normalizeRequiredText(request.adminFirstName(), "Tenant admin first name is required");
        String adminLastName = normalizeRequiredText(request.adminLastName(), "Tenant admin last name is required");
        String adminEmail = normalizeRequiredText(request.adminEmail(), "Tenant admin email is required");
        String adminGender = normalizeGender(request.adminGender());
        if (tenantRepository.findByCodeIgnoreCase(code).isPresent()) {
            throw new ApiException("Tenant code already exists");
        }

        Tenant tenant = new Tenant();
        tenant.setCode(code);
        tenant.setName(request.name().trim());
        tenant = tenantRepository.save(tenant);

        Pharmacy pharmacy = new Pharmacy();
        pharmacy.setTenant(tenant);
        pharmacy.setCode("MAIN");
        pharmacy.setName(request.name().trim() + " Main");
        pharmacy = pharmacyRepository.save(pharmacy);
        tenant.setDefaultPharmacy(pharmacy);
        tenant = tenantRepository.save(tenant);

        User tenantAdmin = new User();
        tenantAdmin.setTenant(tenant);
        tenantAdmin.setUsername(adminUsername);
        tenantAdmin.setPasswordHash(passwordEncoder.encode(request.adminPassword().trim()));
        tenantAdmin.setRoles(Set.of(Role.ADMIN));
        tenantAdmin.setFirstName(adminFirstName);
        tenantAdmin.setLastName(adminLastName);
        tenantAdmin.setEmail(adminEmail);
        tenantAdmin.setGender(adminGender);
        tenantAdmin.setDefaultPharmacy(pharmacy);
        tenantAdmin.getAssignedPharmacies().add(pharmacy);
        userRepository.save(tenantAdmin);

        audit(tenant, "TENANT_CREATED");
        log.info("Created tenant {} ({}) with admin user {}", tenant.getName(), tenant.getCode(), adminUsername);
        return toTenantResponse(tenant);
    }

    @Transactional
    public TenantResponse updateStatus(Long tenantId, UpdateTenantStatusRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ApiException("Tenant not found"));

        tenant.setEnabled(request.enabled());
        Tenant saved = tenantRepository.save(tenant);

        audit(saved, request.enabled() ? "TENANT_ENABLED" : "TENANT_DISABLED");
        log.info("Tenant {} status changed to {}", tenant.getCode(), request.enabled() ? "ENABLED" : "DISABLED");
        return toTenantResponse(saved);
    }

    @Transactional
    public TenantResponse updateConfig(Long tenantId, UpdateTenantConfigRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ApiException("Tenant not found"));

        tenant.setBillingEnabled(request.billingEnabled());
        tenant.setTransactionsEnabled(request.transactionsEnabled());
        tenant.setInventoryEnabled(request.inventoryEnabled());
        tenant.setAnalyticsEnabled(request.analyticsEnabled());
        tenant.setAiAssistantEnabled(request.aiAssistantEnabled());
        if (request.defaultPharmacyId() != null) {
            tenant.setDefaultPharmacy(
                    pharmacyRepository.findByIdAndTenant_Id(request.defaultPharmacyId(), tenantId)
                            .orElseThrow(() -> new ApiException("Default pharmacy not found"))
            );
        }

        Tenant saved = tenantRepository.save(tenant);
        audit(saved, "TENANT_CONFIG_UPDATED");
        log.info("Tenant {} configuration updated", tenant.getCode());
        return toTenantResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<TenantUserAssignmentResponse> listUsers() {
        return userRepository.findAllWithTenantEager().stream()
                .filter(user -> !user.hasRole(Role.SUPER_ADMIN))
                .map(this::toUserAssignmentResponse)
                .toList();
    }

    @Transactional
    public TenantUserAssignmentResponse assignUserToTenant(Long userId, AssignUserTenantRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found"));
        if (user.hasRole(Role.SUPER_ADMIN)) {
            throw new ApiException("Cannot assign SUPER_ADMIN user to a tenant");
        }
        Tenant tenant = tenantRepository.findById(request.tenantId())
                .orElseThrow(() -> new ApiException("Tenant not found"));

        user.setTenant(tenant);
        if (tenant.getDefaultPharmacy() != null) {
            user.setDefaultPharmacy(tenant.getDefaultPharmacy());
            user.getAssignedPharmacies().add(tenant.getDefaultPharmacy());
        }
        return toUserAssignmentResponse(userRepository.save(user));
    }

    private TenantResponse toTenantResponse(Tenant tenant) {
        boolean legacyUnconfigured = isLegacyUnconfigured(tenant);
        return new TenantResponse(
                tenant.getId(),
                tenant.getCode(),
                tenant.getName(),
                tenant.isEnabled(),
                tenant.getDefaultPharmacy() == null ? null : tenant.getDefaultPharmacy().getId(),
                tenant.getLogoData() != null && tenant.getLogoData().length > 0,
                legacyUnconfigured || tenant.isBillingEnabled(),
                legacyUnconfigured || tenant.isTransactionsEnabled(),
                legacyUnconfigured || tenant.isInventoryEnabled(),
                legacyUnconfigured || tenant.isAnalyticsEnabled(),
                legacyUnconfigured || tenant.isAiAssistantEnabled(),
                tenant.getCreatedAt(),
                userRepository.countByTenant_Id(tenant.getId())
        );
    }

    private boolean isLegacyUnconfigured(Tenant tenant) {
        return !tenant.isBillingEnabled()
                && !tenant.isTransactionsEnabled()
                && !tenant.isInventoryEnabled()
                && !tenant.isAnalyticsEnabled()
                && !tenant.isAiAssistantEnabled();
    }

    private TenantUserAssignmentResponse toUserAssignmentResponse(User user) {
        return new TenantUserAssignmentResponse(
                user.getId(),
                user.getUsername(),
                user.isEnabled(),
                user.getTenant() == null ? null : user.getTenant().getId(),
                user.getTenant() == null ? null : user.getTenant().getCode(),
                user.getTenant() == null ? null : user.getTenant().getName()
        );
    }

    private void audit(Tenant tenant, String action) {
        TenantAuditLog logEntry = new TenantAuditLog();
        logEntry.setTenant(tenant);
        logEntry.setAction(action);
        logEntry.setPerformedBy(currentUserService.getCurrentUsername());
        tenantAuditLogRepository.save(logEntry);
    }

    private String normalizeCode(String code) {
        String normalized = code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new ApiException("Tenant code is required");
        }
        return normalized;
    }

    private String normalizeUsername(String username) {
        String normalized = username == null ? "" : username.trim();
        if (normalized.isBlank()) {
            throw new ApiException("Tenant admin username is required");
        }
        if (normalized.contains("@")) {
            throw new ApiException("Tenant admin username must not contain '@'");
        }
        return normalized;
    }

    private String normalizeGender(String gender) {
        String normalized = gender == null ? "" : gender.trim().toUpperCase(Locale.ROOT);
        if (!"MALE".equals(normalized) && !"FEMALE".equals(normalized)) {
            throw new ApiException("Gender must be MALE or FEMALE");
        }
        return normalized;
    }

    private String normalizeRequiredText(String value, String errorMessage) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank()) {
            throw new ApiException(errorMessage);
        }
        return normalized;
    }
}
