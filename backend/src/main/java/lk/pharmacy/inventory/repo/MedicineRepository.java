package lk.pharmacy.inventory.repo;

import lk.pharmacy.inventory.domain.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface MedicineRepository extends JpaRepository<Medicine, Long> {
    List<Medicine> findByQuantityLessThanEqual(int quantity);
    List<Medicine> findByExpiryDateBefore(LocalDate date);
    List<Medicine> findByNameContainingIgnoreCase(String name);
}

