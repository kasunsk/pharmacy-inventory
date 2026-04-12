package lk.pharmacy.inventory.repo;

import lk.pharmacy.inventory.domain.MedicineAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicineAuditLogRepository extends JpaRepository<MedicineAuditLog, Long> {
}
