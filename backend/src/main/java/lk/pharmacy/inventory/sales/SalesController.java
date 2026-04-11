package lk.pharmacy.inventory.sales;

import jakarta.validation.Valid;
import lk.pharmacy.inventory.sales.dto.CreateSaleRequest;
import lk.pharmacy.inventory.sales.dto.SaleBillResponse;
import lk.pharmacy.inventory.sales.dto.SaleTransactionSummaryResponse;
import lk.pharmacy.inventory.sales.dto.SalesPeriod;
import lk.pharmacy.inventory.sales.dto.SalesSummaryResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/sales")
public class SalesController {

    private final SalesService salesService;

    public SalesController(SalesService salesService) {
        this.salesService = salesService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','BILLING')")
    public SaleBillResponse create(@Valid @RequestBody CreateSaleRequest request) {
        return salesService.createSale(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TRANSACTIONS')")
    public List<SaleTransactionSummaryResponse> list(
            @RequestParam(required = false) String transactionId,
            @RequestParam(required = false) String salesPerson,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate
    ) {
        return salesService.findTransactions(transactionId, salesPerson, fromDate, toDate);
    }

    @GetMapping("/{transactionId}")
    @PreAuthorize("hasAnyRole('ADMIN','TRANSACTIONS')")
    public SaleBillResponse getByTransactionId(@PathVariable String transactionId) {
        return salesService.getBillByTransactionId(transactionId);
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN','TRANSACTIONS')")
    public SalesSummaryResponse summary(@RequestParam(defaultValue = "DAY") SalesPeriod period) {
        return salesService.getSalesSummary(period);
    }
}

