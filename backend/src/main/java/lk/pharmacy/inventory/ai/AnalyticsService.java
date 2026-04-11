package lk.pharmacy.inventory.ai;

import lk.pharmacy.inventory.domain.Medicine;
import lk.pharmacy.inventory.repo.MedicineRepository;
import lk.pharmacy.inventory.repo.SaleItemRepository;
import lk.pharmacy.inventory.repo.SaleRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service
public class AnalyticsService {

    private final MedicineRepository medicineRepository;
    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;

    public AnalyticsService(MedicineRepository medicineRepository,
                            SaleRepository saleRepository,
                            SaleItemRepository saleItemRepository) {
        this.medicineRepository = medicineRepository;
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
    }

    public Map<String, Object> lowStock() {
        List<Medicine> medicines = medicineRepository.findByQuantityLessThanEqual(10);
        return Map.of("intent", "low_stock", "count", medicines.size(), "items", medicines);
    }

    public Map<String, Object> todaySales() {
        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);
        Instant start = today.atStartOfDay(zone).toInstant();
        Instant end = today.plusDays(1).atStartOfDay(zone).toInstant();
        BigDecimal total = saleRepository.sumTotalBetween(start, end);
        return Map.of("intent", "today_sales", "date", today.toString(), "total", total);
    }

    public Map<String, Object> topSelling() {
        List<Object[]> rows = saleItemRepository.findTopSelling();
        if (rows.isEmpty()) {
            return Map.of("intent", "top_selling", "message", "No sales data yet");
        }
        Object[] top = rows.get(0);
        return Map.of("intent", "top_selling", "medicine", top[0], "units", top[1]);
    }

    public Map<String, Object> availability(String name) {
        List<Medicine> matches = medicineRepository.findByNameContainingIgnoreCase(name);
        return Map.of("intent", "availability", "name", name, "matches", matches);
    }
}

