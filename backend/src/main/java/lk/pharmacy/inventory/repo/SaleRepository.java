package lk.pharmacy.inventory.repo;

import lk.pharmacy.inventory.domain.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.Instant;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    @Query("select coalesce(sum(s.totalAfterDiscount), 0) from Sale s where s.createdAt between :start and :end")
    BigDecimal sumTotalBetween(Instant start, Instant end);
}

