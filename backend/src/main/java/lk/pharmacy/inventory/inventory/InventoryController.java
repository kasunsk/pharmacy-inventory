package lk.pharmacy.inventory.inventory;

import jakarta.validation.Valid;
import lk.pharmacy.inventory.domain.Medicine;
import lk.pharmacy.inventory.inventory.dto.MedicineRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY')")
    public List<Medicine> list() {
        return inventoryService.list();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY')")
    public Medicine getById(@PathVariable Long id) {
        return inventoryService.getById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY')")
    public Medicine create(@Valid @RequestBody MedicineRequest request) {
        return inventoryService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY')")
    public Medicine update(@PathVariable Long id, @Valid @RequestBody MedicineRequest request) {
        return inventoryService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        inventoryService.delete(id);
    }

    @GetMapping("/alerts/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY')")
    public List<Medicine> lowStock(@RequestParam(defaultValue = "10") int threshold) {
        return inventoryService.lowStock(threshold);
    }

    @GetMapping("/alerts/expiry")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY')")
    public List<Medicine> expiry(@RequestParam(defaultValue = "30") int days) {
        return inventoryService.expiringBefore(LocalDate.now().plusDays(days));
    }
}

