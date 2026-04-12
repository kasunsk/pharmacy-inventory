package lk.pharmacy.inventory.sales;

import jakarta.validation.Valid;
import lk.pharmacy.inventory.sales.dto.CreateSaleRequest;
import lk.pharmacy.inventory.sales.dto.BillingMedicineOptionResponse;
import lk.pharmacy.inventory.sales.dto.SaleBillResponse;
import lk.pharmacy.inventory.sales.dto.SaleTransactionSummaryResponse;
import lk.pharmacy.inventory.sales.dto.SalesPeriod;
import lk.pharmacy.inventory.sales.dto.SalesSummaryResponse;
import lk.pharmacy.inventory.tenant.TenantFeatureGuardService;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/sales")
public class SalesController {

    private final SalesService salesService;
    private final TenantFeatureGuardService tenantFeatureGuardService;

    public SalesController(SalesService salesService,
                           TenantFeatureGuardService tenantFeatureGuardService) {
        this.salesService = salesService;
        this.tenantFeatureGuardService = tenantFeatureGuardService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','BILLING')")
    public SaleBillResponse create(@Valid @RequestBody CreateSaleRequest request) {
        tenantFeatureGuardService.requireBillingEnabled();
        return salesService.createSale(request);
    }

    @GetMapping("/billing-medicines")
    @PreAuthorize("hasAnyRole('ADMIN','BILLING')")
    public List<BillingMedicineOptionResponse> billingMedicines() {
        tenantFeatureGuardService.requireBillingEnabled();
        return salesService.listBillingMedicines();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TRANSACTIONS')")
    public Page<SaleTransactionSummaryResponse> list(
            @RequestParam(required = false) String transactionId,
            @RequestParam(required = false) String salesPerson,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        tenantFeatureGuardService.requireTransactionsEnabled();
        return salesService.findTransactions(transactionId, salesPerson, fromDate, toDate, page, size);
    }

    @GetMapping("/{transactionId}")
    @PreAuthorize("hasAnyRole('ADMIN','TRANSACTIONS')")
    public SaleBillResponse getByTransactionId(@PathVariable String transactionId) {
        tenantFeatureGuardService.requireTransactionsEnabled();
        return salesService.getBillByTransactionId(transactionId);
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public SalesSummaryResponse summary(@RequestParam(defaultValue = "DAY") SalesPeriod period) {
        tenantFeatureGuardService.requireAnalyticsEnabled();
        return salesService.getSalesSummary(period);
    }
}

