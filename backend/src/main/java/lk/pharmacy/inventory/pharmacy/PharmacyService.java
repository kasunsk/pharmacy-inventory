package lk.pharmacy.inventory.pharmacy;

import lk.pharmacy.inventory.domain.Pharmacy;
import lk.pharmacy.inventory.domain.Role;
import lk.pharmacy.inventory.domain.Tenant;
import lk.pharmacy.inventory.domain.User;
import lk.pharmacy.inventory.exception.ApiException;
import lk.pharmacy.inventory.pharmacy.dto.CreatePharmacyRequest;
import lk.pharmacy.inventory.pharmacy.dto.PharmacyResponse;
import lk.pharmacy.inventory.pharmacy.dto.UpdatePharmacyStatusRequest;
import lk.pharmacy.inventory.repo.PharmacyRepository;
import lk.pharmacy.inventory.repo.TenantRepository;
import lk.pharmacy.inventory.repo.UserRepository;
import lk.pharmacy.inventory.util.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class PharmacyService {

    private static final long MAX_LOGO_BYTES = 3L * 1024 * 1024;

    private final PharmacyRepository pharmacyRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public PharmacyService(PharmacyRepository pharmacyRepository,
                           TenantRepository tenantRepository,
                           UserRepository userRepository,
                           CurrentUserService currentUserService) {
        this.pharmacyRepository = pharmacyRepository;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<PharmacyResponse> listTenantPharmacies(Long tenantId, boolean enabledOnly) {
        List<Pharmacy> pharmacies = enabledOnly
                ? pharmacyRepository.findByTenant_IdAndEnabledTrueOrderByNameAsc(tenantId)
                : pharmacyRepository.findByTenant_IdOrderByNameAsc(tenantId);
        return pharmacies.stream().map(this::toResponse).toList();
    }

    @Transactional
    public PharmacyResponse createPharmacy(Long tenantId, CreatePharmacyRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ApiException("Tenant not found"));

        String code = request.code().trim().toUpperCase(Locale.ROOT);
        if (pharmacyRepository.findByTenant_IdAndCodeIgnoreCase(tenantId, code).isPresent()) {
            throw new ApiException("Pharmacy code already exists in this tenant");
        }

        Pharmacy pharmacy = new Pharmacy();
        pharmacy.setTenant(tenant);
        pharmacy.setCode(code);
        pharmacy.setName(request.name().trim());
        pharmacy = pharmacyRepository.save(pharmacy);

        if (tenant.getDefaultPharmacy() == null) {
            tenant.setDefaultPharmacy(pharmacy);
            tenantRepository.save(tenant);
        }
        return toResponse(pharmacy);
    }

    @Transactional
    public PharmacyResponse updateStatus(Long tenantId, Long pharmacyId, UpdatePharmacyStatusRequest request) {
        Pharmacy pharmacy = pharmacyRepository.findByIdAndTenant_Id(pharmacyId, tenantId)
                .orElseThrow(() -> new ApiException("Pharmacy not found"));

        boolean enabled = Boolean.TRUE.equals(request.enabled());
        pharmacy.setEnabled(enabled);
        Pharmacy saved = pharmacyRepository.save(pharmacy);

        if (!enabled) {
            userRepository.findByTenant_Id(tenantId).forEach(user -> {
                if (user.hasRole(Role.ADMIN)) {
                    return;
                }
                Set<lk.pharmacy.inventory.domain.Pharmacy> assignments = new LinkedHashSet<>(user.getAssignedPharmacies());
                assignments.removeIf(item -> item.getId().equals(pharmacyId));
                user.setAssignedPharmacies(assignments);
                if (user.getDefaultPharmacy() != null && user.getDefaultPharmacy().getId().equals(pharmacyId)) {
                    user.setDefaultPharmacy(assignments.stream().findFirst().orElse(null));
                }
            });
            userRepository.flush();
        }

        return toResponse(saved);
    }

    @Transactional
    public PharmacyResponse uploadPharmacyLogo(Long tenantId, Long pharmacyId, MultipartFile file) {
        Pharmacy pharmacy = pharmacyRepository.findByIdAndTenant_Id(pharmacyId, tenantId)
                .orElseThrow(() -> new ApiException("Pharmacy not found"));
        applyLogo(pharmacy, file);
        return toResponse(pharmacyRepository.save(pharmacy));
    }

    @Transactional
    public void uploadTenantLogo(Long tenantId, MultipartFile file) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ApiException("Tenant not found"));
        validateLogo(file);
        tenant.setLogoFileName(file.getOriginalFilename());
        tenant.setLogoContentType(file.getContentType());
        try {
            tenant.setLogoData(file.getBytes());
        } catch (IOException e) {
            throw new ApiException("Unable to read uploaded logo");
        }
        tenantRepository.save(tenant);
    }

    @Transactional(readOnly = true)
    public List<PharmacyResponse> listCurrentUserPharmacies() {
        User user = currentUserService.getCurrentTenantUser();
        if (user.hasRole(Role.ADMIN)) {
            return pharmacyRepository.findByTenant_IdAndEnabledTrueOrderByNameAsc(user.getTenant().getId())
                    .stream()
                    .map(this::toResponse)
                    .toList();
        }
        return user.getAssignedPharmacies().stream()
                .filter(Pharmacy::isEnabled)
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public LogoContent getTenantLogoForAdmin(Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ApiException("Tenant not found"));
        if (tenant.getLogoData() == null || tenant.getLogoData().length == 0) {
            throw new ApiException("Tenant logo not found");
        }
        return new LogoContent(tenant.getLogoData(), resolveContentType(tenant.getLogoContentType()));
    }

    @Transactional(readOnly = true)
    public LogoContent getPharmacyLogoForAdmin(Long tenantId, Long pharmacyId) {
        Pharmacy pharmacy = pharmacyRepository.findByIdAndTenant_Id(pharmacyId, tenantId)
                .orElseThrow(() -> new ApiException("Pharmacy not found"));
        if (pharmacy.getLogoData() == null || pharmacy.getLogoData().length == 0) {
            throw new ApiException("Pharmacy logo not found");
        }
        return new LogoContent(pharmacy.getLogoData(), resolveContentType(pharmacy.getLogoContentType()));
    }

    @Transactional(readOnly = true)
    public LogoContent getCurrentTenantLogo() {
        User user = currentUserService.getCurrentTenantUser();
        Tenant tenant = user.getTenant();
        if (tenant.getLogoData() == null || tenant.getLogoData().length == 0) {
            throw new ApiException("Tenant logo not found");
        }
        return new LogoContent(tenant.getLogoData(), resolveContentType(tenant.getLogoContentType()));
    }

    @Transactional(readOnly = true)
    public LogoContent getCurrentPharmacyLogo() {
        Pharmacy pharmacy = currentUserService.getCurrentPharmacy();
        if (pharmacy.getLogoData() == null || pharmacy.getLogoData().length == 0) {
            throw new ApiException("Pharmacy logo not found");
        }
        return new LogoContent(pharmacy.getLogoData(), resolveContentType(pharmacy.getLogoContentType()));
    }

    private void applyLogo(Pharmacy pharmacy, MultipartFile file) {
        validateLogo(file);
        pharmacy.setLogoFileName(file.getOriginalFilename());
        pharmacy.setLogoContentType(file.getContentType());
        try {
            pharmacy.setLogoData(file.getBytes());
        } catch (IOException e) {
            throw new ApiException("Unable to read uploaded logo");
        }
    }

    private void validateLogo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException("Logo file is required");
        }
        if (file.getSize() > MAX_LOGO_BYTES) {
            throw new ApiException("Logo file is too large");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ApiException("Only image logo files are supported");
        }
    }

    private PharmacyResponse toResponse(Pharmacy pharmacy) {
        return new PharmacyResponse(
                pharmacy.getId(),
                pharmacy.getTenant().getId(),
                pharmacy.getCode(),
                pharmacy.getName(),
                pharmacy.isEnabled(),
                pharmacy.getLogoData() != null && pharmacy.getLogoData().length > 0
        );
    }

    private String resolveContentType(String value) {
        return (value == null || value.isBlank()) ? "image/png" : value;
    }

    public record LogoContent(byte[] data, String contentType) {
    }
}

