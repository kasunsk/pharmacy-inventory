package lk.pharmacy.inventory.repo;

import lk.pharmacy.inventory.domain.SaleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {

    @Query("select si.medicineNameSnapshot, sum(si.quantity) from SaleItem si group by si.medicineNameSnapshot order by sum(si.quantity) desc")
    List<Object[]> findTopSelling();

    @Query("""
            select si.medicineNameSnapshot, sum(si.quantity)
            from SaleItem si
            where si.sale.tenant.id = :tenantId and si.sale.createdAt between :start and :end
            group by si.medicineNameSnapshot
            order by sum(si.quantity) desc
            """)
    List<Object[]> findTopSellingBetween(Long tenantId, Instant start, Instant end);

    @Query("select coalesce(sum(si.lineCost), 0) from SaleItem si where si.sale.tenant.id = :tenantId and si.sale.createdAt between :start and :end")
    BigDecimal sumCostBetween(Long tenantId, Instant start, Instant end);
}

