package lk.pharmacy.inventory.tenant;
import jakarta.validation.Valid;
import lk.pharmacy.inventory.tenant.dto.AssignUserTenantRequest;
import lk.pharmacy.inventory.tenant.dto.CreateTenantRequest;
import lk.pharmacy.inventory.tenant.dto.TenantResponse;
import lk.pharmacy.inventory.tenant.dto.TenantAuditLogResponse;
import lk.pharmacy.inventory.tenant.dto.TenantUserAssignmentResponse;
import lk.pharmacy.inventory.tenant.dto.UpdateTenantConfigRequest;
import lk.pharmacy.inventory.tenant.dto.UpdateTenantStatusRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
@RestController
@RequestMapping({"/admin-portal/tenants", "/super-admin/tenants"})
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class TenantController {
    private final TenantService tenantService;
    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }
    @GetMapping
    public List<TenantResponse> listTenants() {
        return tenantService.listTenants();
    }

    @GetMapping("/audits")
    public List<TenantAuditLogResponse> listAudits(@RequestParam(defaultValue = "50") int limit) {
        return tenantService.listAudits(limit);
    }
    @PostMapping
    public TenantResponse createTenant(@Valid @RequestBody CreateTenantRequest request) {
        return tenantService.createTenant(request);
    }

    @PutMapping("/{tenantId}/status")
    public TenantResponse updateStatus(@PathVariable Long tenantId,
                                       @Valid @RequestBody UpdateTenantStatusRequest request) {
        return tenantService.updateStatus(tenantId, request);
    }

    @PutMapping("/{tenantId}/config")
    public TenantResponse updateConfig(@PathVariable Long tenantId,
                                       @Valid @RequestBody UpdateTenantConfigRequest request) {
        return tenantService.updateConfig(tenantId, request);
    }
    @GetMapping("/users")
    public List<TenantUserAssignmentResponse> listUsers() {
        return tenantService.listUsers();
    }
    @PutMapping("/users/{userId}/assignment")
    public TenantUserAssignmentResponse assignUserToTenant(@PathVariable Long userId,
                                                           @Valid @RequestBody AssignUserTenantRequest request) {
        return tenantService.assignUserToTenant(userId, request);
    }
}
