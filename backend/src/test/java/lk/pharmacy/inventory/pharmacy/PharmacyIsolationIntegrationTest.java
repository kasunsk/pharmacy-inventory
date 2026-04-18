package lk.pharmacy.inventory.pharmacy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lk.pharmacy.inventory.domain.Medicine;
import lk.pharmacy.inventory.domain.Pharmacy;
import lk.pharmacy.inventory.domain.Role;
import lk.pharmacy.inventory.domain.Tenant;
import lk.pharmacy.inventory.domain.User;
import lk.pharmacy.inventory.repo.MedicineRepository;
import lk.pharmacy.inventory.repo.PharmacyRepository;
import lk.pharmacy.inventory.repo.TenantRepository;
import lk.pharmacy.inventory.repo.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PharmacyIsolationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private PharmacyRepository pharmacyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MedicineRepository medicineRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void shouldDenyInventoryReadAcrossPharmaciesForNonAdminUser() throws Exception {
        TestFixture fixture = createFixture();

        User staff = new User();
        staff.setTenant(fixture.tenant);
        staff.setUsername("staff_" + UUID.randomUUID().toString().substring(0, 8));
        staff.setPasswordHash(passwordEncoder.encode("pass123"));
        staff.setRoles(Set.of(Role.INVENTORY));
        staff.setDefaultPharmacy(fixture.pharmacyA);
        staff.getAssignedPharmacies().add(fixture.pharmacyA);
        userRepository.save(staff);

        String token = loginAndExtractToken(staff.getUsername() + "@" + fixture.tenant.getCode(), "pass123");

        mockMvc.perform(get("/inventory/" + fixture.medicineInPharmacyB.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Medicine not found"));
    }

    @Test
    void adminShouldAccessAllTenantPharmaciesBySwitchingPharmacyContext() throws Exception {
        TestFixture fixture = createFixture();

        User admin = new User();
        admin.setTenant(fixture.tenant);
        admin.setUsername("admin_" + UUID.randomUUID().toString().substring(0, 8));
        admin.setPasswordHash(passwordEncoder.encode("pass123"));
        admin.setRoles(Set.of(Role.ADMIN));
        admin.setDefaultPharmacy(fixture.pharmacyA);
        userRepository.save(admin);

        String loginToken = loginAndExtractToken(admin.getUsername() + "@" + fixture.tenant.getCode(), "pass123");

        MvcResult switchResult = mockMvc.perform(post("/auth/pharmacy/select")
                        .header("Authorization", "Bearer " + loginToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pharmacyId\":" + fixture.pharmacyB.getId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.selectedPharmacyId").value(fixture.pharmacyB.getId()))
                .andReturn();

        JsonNode switchBody = objectMapper.readTree(switchResult.getResponse().getContentAsString());
        String switchedToken = switchBody.path("token").asText();
        Assertions.assertFalse(switchedToken.isBlank());

        mockMvc.perform(get("/inventory?page=0&size=10")
                        .header("Authorization", "Bearer " + switchedToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Amoxicillin-B"));
    }

    @Test
    void shouldFallbackToTenantLogoWhenSelectedPharmacyHasNoLogo() throws Exception {
        TestFixture fixture = createFixture();

        User admin = new User();
        admin.setTenant(fixture.tenant);
        admin.setUsername("logo_" + UUID.randomUUID().toString().substring(0, 8));
        admin.setPasswordHash(passwordEncoder.encode("pass123"));
        admin.setRoles(Set.of(Role.ADMIN));
        admin.setDefaultPharmacy(fixture.pharmacyA);
        userRepository.save(admin);

        String token = loginAndExtractToken(admin.getUsername() + "@" + fixture.tenant.getCode(), "pass123");

        mockMvc.perform(get("/branding/pharmacy/logo")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andExpect(content().bytes(fixture.tenantLogoBytes));
    }

    private String loginAndExtractToken(String username, String password) throws Exception {
        String payload = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        return body.path("token").asText();
    }

    private TestFixture createFixture() {
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Tenant tenant = new Tenant();
        tenant.setCode("ISO" + suffix);
        tenant.setName("Isolation Tenant " + suffix);
        byte[] tenantLogo = ("tenant-logo-" + suffix).getBytes(StandardCharsets.UTF_8);
        tenant.setLogoData(tenantLogo);
        tenant.setLogoContentType("image/png");
        tenant = tenantRepository.save(tenant);

        Pharmacy pharmacyA = new Pharmacy();
        pharmacyA.setTenant(tenant);
        pharmacyA.setCode("A" + suffix.substring(0, 3));
        pharmacyA.setName("Branch A");
        pharmacyA = pharmacyRepository.save(pharmacyA);

        Pharmacy pharmacyB = new Pharmacy();
        pharmacyB.setTenant(tenant);
        pharmacyB.setCode("B" + suffix.substring(0, 3));
        pharmacyB.setName("Branch B");
        pharmacyB = pharmacyRepository.save(pharmacyB);

        tenant.setDefaultPharmacy(pharmacyA);
        tenantRepository.save(tenant);

        Medicine medicineInPharmacyA = new Medicine();
        medicineInPharmacyA.setTenant(tenant);
        medicineInPharmacyA.setPharmacy(pharmacyA);
        medicineInPharmacyA.setName("Paracetamol-A");
        medicineInPharmacyA.setBatchNumber("BA-" + suffix);
        medicineInPharmacyA.setExpiryDate(LocalDate.now().plusMonths(12));
        medicineInPharmacyA.setSupplier("Supplier A");
        medicineInPharmacyA.setUnitType("tablet");
        medicineInPharmacyA.setPurchasePrice(new BigDecimal("10.00"));
        medicineInPharmacyA.setSellingPrice(new BigDecimal("15.00"));
        medicineInPharmacyA.setQuantity(100);
        medicineInPharmacyA = medicineRepository.save(medicineInPharmacyA);

        Medicine medicineInPharmacyB = new Medicine();
        medicineInPharmacyB.setTenant(tenant);
        medicineInPharmacyB.setPharmacy(pharmacyB);
        medicineInPharmacyB.setName("Amoxicillin-B");
        medicineInPharmacyB.setBatchNumber("BB-" + suffix);
        medicineInPharmacyB.setExpiryDate(LocalDate.now().plusMonths(12));
        medicineInPharmacyB.setSupplier("Supplier B");
        medicineInPharmacyB.setUnitType("capsule");
        medicineInPharmacyB.setPurchasePrice(new BigDecimal("20.00"));
        medicineInPharmacyB.setSellingPrice(new BigDecimal("30.00"));
        medicineInPharmacyB.setQuantity(80);
        medicineInPharmacyB = medicineRepository.save(medicineInPharmacyB);

        return new TestFixture(tenant, pharmacyA, pharmacyB, medicineInPharmacyA, medicineInPharmacyB, tenantLogo);
    }

    private record TestFixture(Tenant tenant,
                               Pharmacy pharmacyA,
                               Pharmacy pharmacyB,
                               Medicine medicineInPharmacyA,
                               Medicine medicineInPharmacyB,
                               byte[] tenantLogoBytes) {
    }
}

