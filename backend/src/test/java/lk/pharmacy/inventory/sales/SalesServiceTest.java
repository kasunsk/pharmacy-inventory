package lk.pharmacy.inventory.sales;

import lk.pharmacy.inventory.domain.Medicine;
import lk.pharmacy.inventory.domain.Role;
import lk.pharmacy.inventory.domain.Sale;
import lk.pharmacy.inventory.domain.User;
import lk.pharmacy.inventory.repo.MedicineRepository;
import lk.pharmacy.inventory.repo.UserRepository;
import lk.pharmacy.inventory.sales.dto.CreateSaleRequest;
import lk.pharmacy.inventory.sales.dto.SaleItemRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@SpringBootTest
@Transactional
class SalesServiceTest {

    @Autowired
    private SalesService salesService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MedicineRepository medicineRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createSaleShouldDeductStockAndComputeTotals() {
        User employer = new User();
        employer.setUsername("staff1");
        employer.setPasswordHash(passwordEncoder.encode("pass123"));
        employer.setRole(Role.EMPLOYER);
        userRepository.save(employer);

        Medicine med = new Medicine();
        med.setName("Paracetamol");
        med.setBatchNumber("B001");
        med.setExpiryDate(LocalDate.now().plusMonths(6));
        med.setSupplier("ABC Pharma");
        med.setPurchasePrice(new BigDecimal("20.00"));
        med.setSellingPrice(new BigDecimal("30.00"));
        med.setQuantity(100);
        medicineRepository.save(med);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("staff1", null, List.of())
        );

        CreateSaleRequest request = new CreateSaleRequest(
                List.of(new SaleItemRequest(med.getId(), 2)),
                new BigDecimal("10.00")
        );

        Sale sale = salesService.createSale(request);

        Assertions.assertEquals(new BigDecimal("60.00"), sale.getTotalBeforeDiscount());
        Assertions.assertEquals(new BigDecimal("50.00"), sale.getTotalAfterDiscount());
        Assertions.assertEquals(98, medicineRepository.findById(med.getId()).orElseThrow().getQuantity());
    }
}

