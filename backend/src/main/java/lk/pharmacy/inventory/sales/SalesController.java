package lk.pharmacy.inventory.sales;

import jakarta.validation.Valid;
import lk.pharmacy.inventory.domain.Sale;
import lk.pharmacy.inventory.repo.SaleRepository;
import lk.pharmacy.inventory.sales.dto.CreateSaleRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sales")
public class SalesController {

    private final SalesService salesService;
    private final SaleRepository saleRepository;

    public SalesController(SalesService salesService, SaleRepository saleRepository) {
        this.salesService = salesService;
        this.saleRepository = saleRepository;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYER')")
    public Sale create(@Valid @RequestBody CreateSaleRequest request) {
        return salesService.createSale(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYER')")
    public List<Sale> list() {
        return saleRepository.findAll();
    }
}

