package lk.pharmacy.inventory.repo;

import lk.pharmacy.inventory.domain.Pharmacy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PharmacyRepository extends JpaRepository<Pharmacy, Long> {
    List<Pharmacy> findByTenant_IdOrderByNameAsc(Long tenantId);
    List<Pharmacy> findByTenant_IdAndEnabledTrueOrderByNameAsc(Long tenantId);
    Optional<Pharmacy> findByIdAndTenant_Id(Long id, Long tenantId);
    Optional<Pharmacy> findByTenant_IdAndCodeIgnoreCase(Long tenantId, String code);
}

