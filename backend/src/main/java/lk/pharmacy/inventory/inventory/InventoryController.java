package lk.pharmacy.inventory.inventory;

import jakarta.validation.Valid;
import lk.pharmacy.inventory.domain.Medicine;
import lk.pharmacy.inventory.inventory.dto.MedicineRequest;
import lk.pharmacy.inventory.inventory.dto.UpdateMedicineRequest;
import lk.pharmacy.inventory.tenant.TenantFeatureGuardService;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryService inventoryService;
    private final TenantFeatureGuardService tenantFeatureGuardService;

    public InventoryController(InventoryService inventoryService,
                               TenantFeatureGuardService tenantFeatureGuardService) {
        this.inventoryService = inventoryService;
        this.tenantFeatureGuardService = tenantFeatureGuardService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY','BILLING','TRANSACTIONS')")
    public Page<Medicine> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        tenantFeatureGuardService.requireInventoryEnabled();
        return inventoryService.list(page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY','BILLING','TRANSACTIONS')")
    public Medicine getById(@PathVariable Long id) {
        tenantFeatureGuardService.requireInventoryEnabled();
        return inventoryService.getById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY')")
    public Medicine create(@Valid @RequestBody MedicineRequest request) {
        tenantFeatureGuardService.requireInventoryEnabled();
        return inventoryService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY')")
    public Medicine update(@PathVariable Long id, @Valid @RequestBody UpdateMedicineRequest request) {
        tenantFeatureGuardService.requireInventoryEnabled();
        return inventoryService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY')")
    public void delete(@PathVariable Long id) {
        tenantFeatureGuardService.requireInventoryEnabled();
        inventoryService.delete(id);
    }

    @GetMapping("/alerts/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY','BILLING','TRANSACTIONS')")
    public List<Medicine> lowStock(@RequestParam(defaultValue = "10") int threshold) {
        tenantFeatureGuardService.requireInventoryEnabled();
        return inventoryService.lowStock(threshold);
    }

    @GetMapping("/alerts/expiry")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY','BILLING','TRANSACTIONS')")
    public List<Medicine> expiry(@RequestParam(defaultValue = "30") int days) {
        tenantFeatureGuardService.requireInventoryEnabled();
        return inventoryService.expiringBefore(LocalDate.now().plusDays(days));
    }
}

