package lk.pharmacy.inventory.ai;

import lk.pharmacy.inventory.domain.Medicine;
import lk.pharmacy.inventory.repo.MedicineRepository;
import lk.pharmacy.inventory.repo.SaleItemRepository;
import lk.pharmacy.inventory.repo.SaleRepository;
import lk.pharmacy.inventory.util.CurrentUserService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service
public class AnalyticsService {

    private final MedicineRepository medicineRepository;
    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final CurrentUserService currentUserService;

    public AnalyticsService(MedicineRepository medicineRepository,
                            SaleRepository saleRepository,
                            SaleItemRepository saleItemRepository,
                            CurrentUserService currentUserService) {
        this.medicineRepository = medicineRepository;
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
        this.currentUserService = currentUserService;
    }

    public Map<String, Object> lowStock() {
        Long tenantId = currentUserService.getCurrentTenantId();
        Long pharmacyId = currentUserService.getCurrentPharmacy().getId();
        List<Medicine> medicines = medicineRepository.findByQuantityLessThanEqualAndTenant_IdAndPharmacy_Id(10, tenantId, pharmacyId);
        return Map.of("intent", "low_stock", "count", medicines.size(), "items", medicines);
    }

    public Map<String, Object> todaySales() {
        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);
        Instant start = today.atStartOfDay(zone).toInstant();
        Instant end = today.plusDays(1).atStartOfDay(zone).toInstant();
        Long tenantId = currentUserService.getCurrentTenantId();
        Long pharmacyId = currentUserService.getCurrentPharmacy().getId();
        BigDecimal total = saleRepository.sumTotalBetween(tenantId, pharmacyId, start, end);
        return Map.of("intent", "today_sales", "date", today.toString(), "total", total);
    }

    public Map<String, Object> topSelling() {
        Long tenantId = currentUserService.getCurrentTenantId();
        Long pharmacyId = currentUserService.getCurrentPharmacy().getId();
        List<Object[]> rows = saleItemRepository.findTopSellingBetween(tenantId, pharmacyId, Instant.EPOCH, Instant.now().plusSeconds(1));
        if (rows.isEmpty()) {
            return Map.of("intent", "top_selling", "message", "No sales data yet");
        }
        Object[] top = rows.get(0);
        return Map.of("intent", "top_selling", "medicine", top[0], "units", top[1]);
    }

    public Map<String, Object> availability(String name) {
        Long tenantId = currentUserService.getCurrentTenantId();
        Long pharmacyId = currentUserService.getCurrentPharmacy().getId();
        List<Medicine> matches = medicineRepository.findByNameContainingIgnoreCaseAndTenant_IdAndPharmacy_Id(name, tenantId, pharmacyId);
        return Map.of("intent", "availability", "name", name, "matches", matches);
    }
}

