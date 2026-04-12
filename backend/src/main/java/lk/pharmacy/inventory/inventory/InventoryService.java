package lk.pharmacy.inventory.inventory;

import lk.pharmacy.inventory.domain.MedicineAuditLog;
import lk.pharmacy.inventory.domain.Medicine;
import lk.pharmacy.inventory.exception.ApiException;
import lk.pharmacy.inventory.inventory.dto.MedicineRequest;
import lk.pharmacy.inventory.inventory.dto.UpdateMedicineRequest;
import lk.pharmacy.inventory.repo.MedicineAuditLogRepository;
import lk.pharmacy.inventory.repo.MedicineRepository;
import lk.pharmacy.inventory.util.CurrentUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
public class InventoryService {

    private final MedicineRepository medicineRepository;
    private final MedicineAuditLogRepository auditLogRepository;
    private final CurrentUserService currentUserService;

    public InventoryService(MedicineRepository medicineRepository,
                            MedicineAuditLogRepository auditLogRepository,
                            CurrentUserService currentUserService) {
        this.medicineRepository = medicineRepository;
        this.auditLogRepository = auditLogRepository;
        this.currentUserService = currentUserService;
    }

    public Page<Medicine> list(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Long tenantId = currentUserService.getCurrentUser().getTenant().getId();
        return medicineRepository.findByTenant_Id(tenantId, PageRequest.of(safePage, safeSize, Sort.by("name").ascending()));
    }

    public Medicine getById(Long id) {
        Long tenantId = currentUserService.getCurrentUser().getTenant().getId();
        return medicineRepository.findByIdAndTenant_Id(id, tenantId)
                .orElseThrow(() -> new ApiException("Medicine not found"));
    }

    public Medicine create(MedicineRequest request) {
        Medicine medicine = new Medicine();
        medicine.setTenant(currentUserService.getCurrentUser().getTenant());
        apply(medicine, request);
        return medicineRepository.save(medicine);
    }

    public Medicine update(Long id, UpdateMedicineRequest request) {
        Long tenantId = currentUserService.getCurrentUser().getTenant().getId();
        Medicine medicine = medicineRepository.findByIdAndTenant_Id(id, tenantId)
                .orElseThrow(() -> new ApiException("Medicine not found"));
        apply(medicine, request.toMedicineRequest());
        Medicine saved = medicineRepository.save(medicine);

        MedicineAuditLog auditLog = new MedicineAuditLog();
        auditLog.setMedicine(saved);
        auditLog.setModificationReason(request.modificationReason().trim());
        auditLog.setModifiedAt(Instant.now());
        auditLogRepository.save(auditLog);

        return saved;
    }

    public void delete(Long id) {
        Long tenantId = currentUserService.getCurrentUser().getTenant().getId();
        Medicine medicine = medicineRepository.findByIdAndTenant_Id(id, tenantId)
                .orElseThrow(() -> new ApiException("Medicine not found"));
        medicineRepository.delete(medicine);
    }

    public List<Medicine> lowStock(int threshold) {
        Long tenantId = currentUserService.getCurrentUser().getTenant().getId();
        return medicineRepository.findByQuantityLessThanEqualAndTenant_Id(threshold, tenantId);
    }

    public List<Medicine> expiringBefore(LocalDate date) {
        Long tenantId = currentUserService.getCurrentUser().getTenant().getId();
        return medicineRepository.findByExpiryDateBeforeAndTenant_Id(date, tenantId);
    }

    private void apply(Medicine medicine, MedicineRequest request) {
        medicine.setName(request.name());
        medicine.setBatchNumber(request.batchNumber());
        medicine.setExpiryDate(request.expiryDate());
        medicine.setSupplier(request.supplier());
        medicine.setUnitType(request.unitType().trim());
        medicine.setPurchasePrice(request.purchasePrice());
        medicine.setSellingPrice(request.sellingPrice());
        medicine.setQuantity(request.quantity());
    }
}

