package lk.pharmacy.inventory.repo;

import lk.pharmacy.inventory.domain.SaleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {

    @Query("select si.medicineNameSnapshot, sum(si.quantity) from SaleItem si group by si.medicineNameSnapshot order by sum(si.quantity) desc")
    List<Object[]> findTopSelling();
}

