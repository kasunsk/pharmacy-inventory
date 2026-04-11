package lk.pharmacy.inventory.inventory;

import lk.pharmacy.inventory.domain.Medicine;
import lk.pharmacy.inventory.exception.ApiException;
import lk.pharmacy.inventory.inventory.dto.MedicineRequest;
import lk.pharmacy.inventory.repo.MedicineRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class InventoryService {

    private final MedicineRepository medicineRepository;

    public InventoryService(MedicineRepository medicineRepository) {
        this.medicineRepository = medicineRepository;
    }

    public List<Medicine> list() {
        return medicineRepository.findAll();
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

    public Medicine update(Long id, MedicineRequest request) {
        Medicine medicine = medicineRepository.findById(id)
                .orElseThrow(() -> new ApiException("Medicine not found"));
        apply(medicine, request);
        return medicineRepository.save(medicine);
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
        medicine.setPurchasePrice(request.purchasePrice());
        medicine.setSellingPrice(request.sellingPrice());
        medicine.setQuantity(request.quantity());
    }
}

