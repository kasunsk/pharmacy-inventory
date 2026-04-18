package lk.pharmacy.inventory.sales;

import lk.pharmacy.inventory.domain.Medicine;
import lk.pharmacy.inventory.domain.Pharmacy;
import lk.pharmacy.inventory.domain.Role;
import lk.pharmacy.inventory.domain.Tenant;
import lk.pharmacy.inventory.domain.User;
import lk.pharmacy.inventory.exception.ApiException;
import lk.pharmacy.inventory.repo.MedicineRepository;
import lk.pharmacy.inventory.repo.PharmacyRepository;
import lk.pharmacy.inventory.repo.TenantRepository;
import lk.pharmacy.inventory.repo.UserRepository;
import lk.pharmacy.inventory.security.TenantUserPrincipal;
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

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private PharmacyRepository pharmacyRepository;

    private Pharmacy ensurePharmacy(Tenant tenant) {
        Pharmacy pharmacy = pharmacyRepository.findByTenant_IdAndCodeIgnoreCase(tenant.getId(), "MAIN")
                .orElseGet(() -> {
                    Pharmacy created = new Pharmacy();
                    created.setTenant(tenant);
                    created.setCode("MAIN");
                    created.setName("Main");
                    return pharmacyRepository.save(created);
                });
        if (tenant.getDefaultPharmacy() == null) {
            tenant.setDefaultPharmacy(pharmacy);
            tenantRepository.save(tenant);
        }
        return pharmacy;
    }

    private void authenticate(User user, Long pharmacyId) {
        TenantUserPrincipal principal = new TenantUserPrincipal(
                user.getId(),
                user.getTenant().getId(),
                user.getTenant().getCode(),
                pharmacyId,
                user.getUsername(),
                user.getPasswordHash(),
                user.isEnabled(),
                List.<org.springframework.security.core.GrantedAuthority>of()
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of())
        );
    }

    private Tenant ensureTenant() {
        return tenantRepository.findByCode("TEST")
                .orElseGet(() -> {
                    Tenant tenant = new Tenant();
                    tenant.setCode("TEST");
                    tenant.setName("Test Pharmacy");
                    return tenantRepository.save(tenant);
                });
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createSaleShouldDeductStockAndComputeTotals() {
        Tenant tenant = ensureTenant();
        Pharmacy pharmacy = ensurePharmacy(tenant);
        User employer = new User();
        employer.setTenant(tenant);
        employer.setUsername("staff1");
        employer.setPasswordHash(passwordEncoder.encode("pass123"));
        employer.setRoles(Set.of(Role.BILLING));
        employer.setDefaultPharmacy(pharmacy);
        employer.getAssignedPharmacies().add(pharmacy);
        userRepository.save(employer);

        Medicine med = new Medicine();
        med.setTenant(tenant);
        med.setPharmacy(pharmacy);
        med.setName("Paracetamol");
        med.setBatchNumber("B001");
        med.setExpiryDate(LocalDate.now().plusMonths(6));
        med.setSupplier("ABC Pharma");
        med.setPurchasePrice(new BigDecimal("20.00"));
        med.setSellingPrice(new BigDecimal("30.00"));
        med.setQuantity(100);
        medicineRepository.save(med);

        authenticate(employer, pharmacy.getId());

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
        Tenant tenant = ensureTenant();
        Pharmacy pharmacy = ensurePharmacy(tenant);
        User employer = new User();
        employer.setTenant(tenant);
        employer.setUsername("staff2");
        employer.setPasswordHash(passwordEncoder.encode("pass123"));
        employer.setRoles(Set.of(Role.BILLING));
        employer.setDefaultPharmacy(pharmacy);
        employer.getAssignedPharmacies().add(pharmacy);
        userRepository.save(employer);

        Medicine med = new Medicine();
        med.setTenant(tenant);
        med.setPharmacy(pharmacy);
        med.setName("Vitamin C");
        med.setBatchNumber("B090");
        med.setExpiryDate(LocalDate.now().plusMonths(12));
        med.setSupplier("Medi House");
        med.setPurchasePrice(new BigDecimal("50.00"));
        med.setSellingPrice(new BigDecimal("80.00"));
        med.setQuantity(40);
        medicineRepository.save(med);

        authenticate(employer, pharmacy.getId());

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
        Tenant tenant = ensureTenant();
        Pharmacy pharmacy = ensurePharmacy(tenant);
        User employer = new User();
        employer.setTenant(tenant);
        employer.setUsername("staff3");
        employer.setPasswordHash(passwordEncoder.encode("pass123"));
        employer.setRoles(Set.of(Role.BILLING));
        employer.setDefaultPharmacy(pharmacy);
        employer.getAssignedPharmacies().add(pharmacy);
        userRepository.save(employer);

        Medicine med = new Medicine();
        med.setTenant(tenant);
        med.setPharmacy(pharmacy);
        med.setName("Ibuprofen");
        med.setBatchNumber("B099");
        med.setExpiryDate(LocalDate.now().plusMonths(10));
        med.setSupplier("ABC Pharma");
        med.setPurchasePrice(new BigDecimal("30.00"));
        med.setSellingPrice(new BigDecimal("45.00"));
        med.setQuantity(30);
        medicineRepository.save(med);

        authenticate(employer, pharmacy.getId());

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

