package lk.pharmacy.inventory.pharmacy;
import jakarta.validation.Valid;
import lk.pharmacy.inventory.pharmacy.dto.CreatePharmacyRequest;
import lk.pharmacy.inventory.pharmacy.dto.PharmacyResponse;
import lk.pharmacy.inventory.pharmacy.dto.UpdatePharmacyStatusRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
@RestController
@RequestMapping
public class PharmacyController {
    private final PharmacyService pharmacyService;
    public PharmacyController(PharmacyService pharmacyService) {
        this.pharmacyService = pharmacyService;
    }
    @GetMapping("/admin-portal/tenants/{tenantId}/pharmacies")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public List<PharmacyResponse> listTenantPharmacies(@PathVariable Long tenantId,
                                                       @RequestParam(defaultValue = "false") boolean enabledOnly) {
        return pharmacyService.listTenantPharmacies(tenantId, enabledOnly);
    }
    @PostMapping("/admin-portal/tenants/{tenantId}/pharmacies")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public PharmacyResponse createPharmacy(@PathVariable Long tenantId,
                                           @Valid @RequestBody CreatePharmacyRequest request) {
        return pharmacyService.createPharmacy(tenantId, request);
    }
    @PutMapping("/admin-portal/tenants/{tenantId}/pharmacies/{pharmacyId}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public PharmacyResponse updatePharmacyStatus(@PathVariable Long tenantId,
                                                 @PathVariable Long pharmacyId,
                                                 @Valid @RequestBody UpdatePharmacyStatusRequest request) {
        return pharmacyService.updateStatus(tenantId, pharmacyId, request);
    }
    @PostMapping("/admin-portal/tenants/{tenantId}/logo")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public void uploadTenantLogo(@PathVariable Long tenantId, @RequestParam("file") MultipartFile file) {
        pharmacyService.uploadTenantLogo(tenantId, file);
    }
    @PostMapping("/admin-portal/tenants/{tenantId}/pharmacies/{pharmacyId}/logo")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public PharmacyResponse uploadPharmacyLogo(@PathVariable Long tenantId,
                                               @PathVariable Long pharmacyId,
                                               @RequestParam("file") MultipartFile file) {
        return pharmacyService.uploadPharmacyLogo(tenantId, pharmacyId, file);
    }
    @GetMapping("/pharmacies/my")
    @PreAuthorize("isAuthenticated()")
    public List<PharmacyResponse> listMyPharmacies() {
        return pharmacyService.listCurrentUserPharmacies();
    }

    @GetMapping("/admin-portal/tenants/{tenantId}/logo")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<byte[]> viewTenantLogo(@PathVariable Long tenantId) {
        PharmacyService.LogoContent logo = pharmacyService.getTenantLogoForAdmin(tenantId);
        return toLogoResponse(logo);
    }

    @GetMapping("/admin-portal/tenants/{tenantId}/pharmacies/{pharmacyId}/logo")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<byte[]> viewPharmacyLogo(@PathVariable Long tenantId,
                                                   @PathVariable Long pharmacyId) {
        PharmacyService.LogoContent logo = pharmacyService.getPharmacyLogoForAdmin(tenantId, pharmacyId);
        return toLogoResponse(logo);
    }

    @GetMapping("/branding/tenant/logo")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> currentTenantLogo() {
        PharmacyService.LogoContent logo = pharmacyService.getCurrentTenantLogo();
        return toLogoResponse(logo);
    }

    @GetMapping("/branding/pharmacy/logo")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> currentPharmacyLogo() {
        PharmacyService.LogoContent logo = pharmacyService.getCurrentPharmacyLogo();
        return toLogoResponse(logo);
    }

    private ResponseEntity<byte[]> toLogoResponse(PharmacyService.LogoContent logo) {
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(logo.contentType());
        } catch (Exception ignored) {
            mediaType = MediaType.IMAGE_PNG;
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .contentType(mediaType)
                .body(logo.data());
    }
}
