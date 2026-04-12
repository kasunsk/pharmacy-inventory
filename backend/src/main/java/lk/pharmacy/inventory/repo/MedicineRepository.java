package lk.pharmacy.inventory.repo;

import lk.pharmacy.inventory.domain.Medicine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MedicineRepository extends JpaRepository<Medicine, Long> {
    Page<Medicine> findByTenant_Id(Long tenantId, Pageable pageable);
    Optional<Medicine> findByIdAndTenant_Id(Long id, Long tenantId);
    boolean existsByIdAndTenant_Id(Long id, Long tenantId);
    List<Medicine> findByTenant_Id(Long tenantId);
    List<Medicine> findByTenantIsNull();
    List<Medicine> findByQuantityLessThanEqualAndTenant_Id(int quantity, Long tenantId);
    List<Medicine> findByExpiryDateBeforeAndTenant_Id(LocalDate date, Long tenantId);
    List<Medicine> findByNameContainingIgnoreCaseAndTenant_Id(String name, Long tenantId);
}

