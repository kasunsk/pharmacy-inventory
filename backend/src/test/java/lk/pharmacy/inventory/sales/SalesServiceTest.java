package lk.pharmacy.inventory.sales;

import lk.pharmacy.inventory.domain.Medicine;
import lk.pharmacy.inventory.domain.Role;
import lk.pharmacy.inventory.domain.User;
import lk.pharmacy.inventory.exception.ApiException;
import lk.pharmacy.inventory.repo.MedicineRepository;
import lk.pharmacy.inventory.repo.UserRepository;
import lk.pharmacy.inventory.sales.dto.CreateSaleRequest;
import lk.pharmacy.inventory.sales.dto.SaleItemRequest;
import lk.pharmacy.inventory.sales.dto.SaleBillResponse;
import lk.pharmacy.inventory.sales.dto.SalesPeriod;
import lk.pharmacy.inventory.sales.dto.SalesSummaryResponse;
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
import java.util.Set;

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
        employer.setRoles(Set.of(Role.BILLING));
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
                List.of(new SaleItemRequest(med.getId(), med.getName(), 2, "tablets", null, false, "1 tablet per day (morning)", null, null)),
                new BigDecimal("10.00"),
                "Kamal",
                "0771234567"
        );

        SaleBillResponse sale = salesService.createSale(request);

        Assertions.assertEquals(new BigDecimal("60.00"), sale.totalBeforeDiscount());
        Assertions.assertEquals(new BigDecimal("50.00"), sale.totalAmount());
        Assertions.assertEquals("tablets", sale.items().get(0).unitType());
        Assertions.assertEquals("1 tablet per day (morning)", sale.items().get(0).dosageInstruction());
        Assertions.assertEquals(98, medicineRepository.findById(med.getId()).orElseThrow().getQuantity());
    }

    @Test
    void salesSummaryShouldReturnRevenueCostAndProfit() {
        User employer = new User();
        employer.setUsername("staff2");
        employer.setPasswordHash(passwordEncoder.encode("pass123"));
        employer.setRoles(Set.of(Role.BILLING));
        userRepository.save(employer);

        Medicine med = new Medicine();
        med.setName("Vitamin C");
        med.setBatchNumber("B090");
        med.setExpiryDate(LocalDate.now().plusMonths(12));
        med.setSupplier("Medi House");
        med.setPurchasePrice(new BigDecimal("50.00"));
        med.setSellingPrice(new BigDecimal("80.00"));
        med.setQuantity(40);
        medicineRepository.save(med);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("staff2", null, List.of())
        );

        salesService.createSale(new CreateSaleRequest(
                List.of(new SaleItemRequest(med.getId(), med.getName(), 2, "capsules", null, false, "2 tablets per day (morning and evening after food)", null, null)),
                new BigDecimal("20.00"),
                null,
                null
        ));

        SalesSummaryResponse summary = salesService.getSalesSummary(SalesPeriod.DAY);

        Assertions.assertEquals(1, summary.saleCount());
        Assertions.assertEquals(new BigDecimal("140.00"), summary.totalSales());
        Assertions.assertEquals(new BigDecimal("100.00"), summary.totalCost());
        Assertions.assertEquals(new BigDecimal("40.00"), summary.totalProfit());
    }

    @Test
    void shouldRejectManualPriceOverrideWhenNotAllowed() {
        User employer = new User();
        employer.setUsername("staff3");
        employer.setPasswordHash(passwordEncoder.encode("pass123"));
        employer.setRoles(Set.of(Role.BILLING));
        userRepository.save(employer);

        Medicine med = new Medicine();
        med.setName("Ibuprofen");
        med.setBatchNumber("B099");
        med.setExpiryDate(LocalDate.now().plusMonths(10));
        med.setSupplier("ABC Pharma");
        med.setPurchasePrice(new BigDecimal("30.00"));
        med.setSellingPrice(new BigDecimal("45.00"));
        med.setQuantity(30);
        medicineRepository.save(med);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("staff3", null, List.of())
        );

        ApiException ex = Assertions.assertThrows(ApiException.class, () ->
                salesService.createSale(new CreateSaleRequest(
                        List.of(new SaleItemRequest(med.getId(), med.getName(), 1, "tablets", new BigDecimal("40.00"), false, "1 tablet per day (morning)", null, null)),
                        BigDecimal.ZERO,
                        null,
                        null
                ))
        );

        Assertions.assertTrue(ex.getMessage().contains("not allowed"));
    }
}

