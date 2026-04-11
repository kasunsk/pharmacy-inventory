package lk.pharmacy.inventory.repo;

import lk.pharmacy.inventory.domain.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    @Query("select coalesce(sum(s.totalAfterDiscount), 0) from Sale s where s.createdAt between :start and :end")
    BigDecimal sumTotalBetween(Instant start, Instant end);

    long countByCreatedAtBetween(Instant start, Instant end);

    Optional<Sale> findByTransactionId(String transactionId);

    List<Sale> findByCreatedAtBetweenOrderByCreatedAtDesc(Instant start, Instant end);

    List<Sale> findByCreatedAtBetweenAndCreatedBy_UsernameContainingIgnoreCaseOrderByCreatedAtDesc(
            Instant start,
            Instant end,
            String username
    );

    List<Sale> findByTransactionIdContainingIgnoreCaseOrderByCreatedAtDesc(String transactionId);

    List<Sale> findByTransactionIdContainingIgnoreCaseAndCreatedAtBetweenOrderByCreatedAtDesc(
            String transactionId,
            Instant start,
            Instant end
    );

    List<Sale> findByTransactionIdContainingIgnoreCaseAndCreatedAtBetweenAndCreatedBy_UsernameContainingIgnoreCaseOrderByCreatedAtDesc(
            String transactionId,
            Instant start,
            Instant end,
            String username
    );

    @Query("""
            select s.createdBy.username, count(s), coalesce(sum(s.totalAfterDiscount), 0)
            from Sale s
            where s.createdAt between :start and :end
            group by s.createdBy.username
            order by coalesce(sum(s.totalAfterDiscount), 0) desc
            """)
    List<Object[]> summarizeSalesByUser(Instant start, Instant end);
}

