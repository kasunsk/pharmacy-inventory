package lk.pharmacy.inventory.inventory;

import lk.pharmacy.inventory.domain.MedicineAuditLog;
import lk.pharmacy.inventory.domain.Medicine;
import lk.pharmacy.inventory.exception.ApiException;
import lk.pharmacy.inventory.inventory.dto.MedicineRequest;
import lk.pharmacy.inventory.inventory.dto.UpdateMedicineRequest;
import lk.pharmacy.inventory.repo.MedicineAuditLogRepository;
import lk.pharmacy.inventory.repo.MedicineRepository;
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

    public InventoryService(MedicineRepository medicineRepository, MedicineAuditLogRepository auditLogRepository) {
        this.medicineRepository = medicineRepository;
        this.auditLogRepository = auditLogRepository;
    }

    public Page<Medicine> list(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        return medicineRepository.findAll(PageRequest.of(safePage, safeSize, Sort.by("name").ascending()));
    }

    public Medicine getById(Long id) {
        return medicineRepository.findById(id)
                .orElseThrow(() -> new ApiException("Medicine not found"));
    }

    public Medicine create(MedicineRequest request) {
        Medicine medicine = new Medicine();
        apply(medicine, request);
        return medicineRepository.save(medicine);
    }

    public Medicine update(Long id, UpdateMedicineRequest request) {
        Medicine medicine = medicineRepository.findById(id)
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
        if (!medicineRepository.existsById(id)) {
            throw new ApiException("Medicine not found");
        }
        medicineRepository.deleteById(id);
    }

    public List<Medicine> lowStock(int threshold) {
        return medicineRepository.findByQuantityLessThanEqual(threshold);
    }

    public List<Medicine> expiringBefore(LocalDate date) {
        return medicineRepository.findByExpiryDateBefore(date);
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

