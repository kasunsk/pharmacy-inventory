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
        Long tenantId = currentUserService.getCurrentTenantId();
        Long pharmacyId = currentUserService.getCurrentPharmacy().getId();
        return medicineRepository.findByTenant_IdAndPharmacy_Id(tenantId, pharmacyId, PageRequest.of(safePage, safeSize, Sort.by("name").ascending()));
    }

    public Medicine getById(Long id) {
        Long tenantId = currentUserService.getCurrentTenantId();
        Long pharmacyId = currentUserService.getCurrentPharmacy().getId();
        return medicineRepository.findByIdAndTenant_IdAndPharmacy_Id(id, tenantId, pharmacyId)
                .orElseThrow(() -> new ApiException("Medicine not found"));
    }

    public Medicine create(MedicineRequest request) {
        var currentUser = currentUserService.getCurrentTenantUser();
        Medicine medicine = new Medicine();
        medicine.setTenant(currentUser.getTenant());
        medicine.setPharmacy(currentUserService.getCurrentPharmacy());
        apply(medicine, request);
        return medicineRepository.save(medicine);
    }

    public Medicine update(Long id, UpdateMedicineRequest request) {
        Long tenantId = currentUserService.getCurrentTenantId();
        Long pharmacyId = currentUserService.getCurrentPharmacy().getId();
        Medicine medicine = medicineRepository.findByIdAndTenant_IdAndPharmacy_Id(id, tenantId, pharmacyId)
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
        Long tenantId = currentUserService.getCurrentTenantId();
        Long pharmacyId = currentUserService.getCurrentPharmacy().getId();
        Medicine medicine = medicineRepository.findByIdAndTenant_IdAndPharmacy_Id(id, tenantId, pharmacyId)
                .orElseThrow(() -> new ApiException("Medicine not found"));
        medicineRepository.delete(medicine);
    }

    public List<Medicine> lowStock(int threshold) {
        Long tenantId = currentUserService.getCurrentTenantId();
        Long pharmacyId = currentUserService.getCurrentPharmacy().getId();
        return medicineRepository.findByQuantityLessThanEqualAndTenant_IdAndPharmacy_Id(threshold, tenantId, pharmacyId);
    }

    public List<Medicine> expiringBefore(LocalDate date) {
        Long tenantId = currentUserService.getCurrentTenantId();
        Long pharmacyId = currentUserService.getCurrentPharmacy().getId();
        return medicineRepository.findByExpiryDateBeforeAndTenant_IdAndPharmacy_Id(date, tenantId, pharmacyId);
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

