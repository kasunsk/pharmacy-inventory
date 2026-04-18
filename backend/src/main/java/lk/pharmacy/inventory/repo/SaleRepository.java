package lk.pharmacy.inventory.repo;

import lk.pharmacy.inventory.domain.Sale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    @Query("select coalesce(sum(s.totalAfterDiscount), 0) from Sale s where s.tenant.id = :tenantId and s.createdAt between :start and :end")
    BigDecimal sumTotalBetween(Long tenantId, Instant start, Instant end);

    @Query("select coalesce(sum(s.totalAfterDiscount), 0) from Sale s where s.tenant.id = :tenantId and s.pharmacy.id = :pharmacyId and s.createdAt between :start and :end")
    BigDecimal sumTotalBetween(Long tenantId, Long pharmacyId, Instant start, Instant end);

    long countByTenant_IdAndCreatedAtBetween(Long tenantId, Instant start, Instant end);

    long countByTenant_IdAndPharmacy_IdAndCreatedAtBetween(Long tenantId, Long pharmacyId, Instant start, Instant end);

    Optional<Sale> findByTransactionIdAndTenant_Id(String transactionId, Long tenantId);

    Optional<Sale> findByTransactionIdAndTenant_IdAndPharmacy_Id(String transactionId, Long tenantId, Long pharmacyId);

    Optional<Sale> findByIdAndTenant_Id(Long id, Long tenantId);

    Optional<Sale> findByIdAndTenant_IdAndPharmacy_Id(Long id, Long tenantId, Long pharmacyId);

    List<Sale> findByTenantIsNull();

    List<Sale> findByTenant_IdAndCreatedAtBetweenOrderByCreatedAtDesc(Long tenantId, Instant start, Instant end);

    List<Sale> findByTenant_IdAndPharmacy_IdAndCreatedAtBetweenOrderByCreatedAtDesc(Long tenantId, Long pharmacyId, Instant start, Instant end);

    Page<Sale> findByTenant_IdAndCreatedAtBetweenOrderByCreatedAtDesc(Long tenantId, Instant start, Instant end, Pageable pageable);

    Page<Sale> findByTenant_IdAndPharmacy_IdAndCreatedAtBetweenOrderByCreatedAtDesc(Long tenantId, Long pharmacyId, Instant start, Instant end, Pageable pageable);

    List<Sale> findByTenant_IdAndCreatedAtBetweenAndCreatedBy_UsernameContainingIgnoreCaseOrderByCreatedAtDesc(
            Long tenantId,
            Instant start,
            Instant end,
            String username
    );

    List<Sale> findByTenant_IdAndPharmacy_IdAndCreatedAtBetweenAndCreatedBy_UsernameContainingIgnoreCaseOrderByCreatedAtDesc(
            Long tenantId,
            Long pharmacyId,
            Instant start,
            Instant end,
            String username
    );

    Page<Sale> findByTenant_IdAndCreatedAtBetweenAndCreatedBy_UsernameContainingIgnoreCaseOrderByCreatedAtDesc(
            Long tenantId,
            Instant start,
            Instant end,
            String username,
            Pageable pageable
    );

    Page<Sale> findByTenant_IdAndPharmacy_IdAndCreatedAtBetweenAndCreatedBy_UsernameContainingIgnoreCaseOrderByCreatedAtDesc(
            Long tenantId,
            Long pharmacyId,
            Instant start,
            Instant end,
            String username,
            Pageable pageable
    );

    List<Sale> findByTenant_IdAndTransactionIdContainingIgnoreCaseAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long tenantId,
            String transactionId,
            Instant start,
            Instant end
    );

    List<Sale> findByTenant_IdAndPharmacy_IdAndTransactionIdContainingIgnoreCaseAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long tenantId,
            Long pharmacyId,
            String transactionId,
            Instant start,
            Instant end
    );

    Page<Sale> findByTenant_IdAndTransactionIdContainingIgnoreCaseAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long tenantId,
            String transactionId,
            Instant start,
            Instant end,
            Pageable pageable
    );

    Page<Sale> findByTenant_IdAndPharmacy_IdAndTransactionIdContainingIgnoreCaseAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long tenantId,
            Long pharmacyId,
            String transactionId,
            Instant start,
            Instant end,
            Pageable pageable
    );

    List<Sale> findByTenant_IdAndTransactionIdContainingIgnoreCaseAndCreatedAtBetweenAndCreatedBy_UsernameContainingIgnoreCaseOrderByCreatedAtDesc(
            Long tenantId,
            String transactionId,
            Instant start,
            Instant end,
            String username
    );

    List<Sale> findByTenant_IdAndPharmacy_IdAndTransactionIdContainingIgnoreCaseAndCreatedAtBetweenAndCreatedBy_UsernameContainingIgnoreCaseOrderByCreatedAtDesc(
            Long tenantId,
            Long pharmacyId,
            String transactionId,
            Instant start,
            Instant end,
            String username
    );

    Page<Sale> findByTenant_IdAndTransactionIdContainingIgnoreCaseAndCreatedAtBetweenAndCreatedBy_UsernameContainingIgnoreCaseOrderByCreatedAtDesc(
            Long tenantId,
            String transactionId,
            Instant start,
            Instant end,
            String username,
            Pageable pageable
    );

    Page<Sale> findByTenant_IdAndPharmacy_IdAndTransactionIdContainingIgnoreCaseAndCreatedAtBetweenAndCreatedBy_UsernameContainingIgnoreCaseOrderByCreatedAtDesc(
            Long tenantId,
            Long pharmacyId,
            String transactionId,
            Instant start,
            Instant end,
            String username,
            Pageable pageable
    );

    @Query("""
            select s.createdBy.username, count(s), coalesce(sum(s.totalAfterDiscount), 0)
            from Sale s
            where s.tenant.id = :tenantId and s.createdAt between :start and :end
            group by s.createdBy.username
            order by coalesce(sum(s.totalAfterDiscount), 0) desc
            """)
    List<Object[]> summarizeSalesByUser(Long tenantId, Instant start, Instant end);

    @Query("""
            select s.createdBy.username, count(s), coalesce(sum(s.totalAfterDiscount), 0)
            from Sale s
            where s.tenant.id = :tenantId and s.pharmacy.id = :pharmacyId and s.createdAt between :start and :end
            group by s.createdBy.username
            order by coalesce(sum(s.totalAfterDiscount), 0) desc
            """)
    List<Object[]> summarizeSalesByUser(Long tenantId, Long pharmacyId, Instant start, Instant end);
}

