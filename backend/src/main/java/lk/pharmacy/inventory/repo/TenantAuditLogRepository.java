package lk.pharmacy.inventory.repo;

import lk.pharmacy.inventory.domain.TenantAuditLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TenantAuditLogRepository extends JpaRepository<TenantAuditLog, Long> {
	List<TenantAuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}


