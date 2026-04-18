package lk.pharmacy.inventory.inventory;

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
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InventoryAlertsIntegrationTest {

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
    void shouldReturnAlertSummaryForCurrentPharmacyOnly() throws Exception {
        TestFixture fixture = createFixture();

        User user = createInventoryUser(fixture.tenant, fixture.pharmacyA, fixture.pharmacyB);
        String token = loginAndExtractToken(user.getUsername() + "@" + fixture.tenant.getCode(), "pass123");

        mockMvc.perform(get("/inventory/alerts/summary?lowStockThreshold=10&expiryDays=30")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lowStockCount").value(1))
                .andExpect(jsonPath("$.expiringSoonCount").value(1))
                .andExpect(jsonPath("$.lowStockThreshold").value(10))
                .andExpect(jsonPath("$.expiryWithinDays").value(30));

        MvcResult switchResult = mockMvc.perform(post("/auth/pharmacy/select")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pharmacyId\":" + fixture.pharmacyB.getId() + "}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode switchedBody = objectMapper.readTree(switchResult.getResponse().getContentAsString());
        String switchedToken = switchedBody.path("token").asText();

        mockMvc.perform(get("/inventory/alerts/summary?lowStockThreshold=10&expiryDays=30")
                        .header("Authorization", "Bearer " + switchedToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lowStockCount").value(1))
                .andExpect(jsonPath("$.expiringSoonCount").value(1));
    }

    @Test
    void shouldRejectAlertsSummaryWhenInventoryFeatureIsDisabled() throws Exception {
        TestFixture fixture = createFixture();
        fixture.tenant.setInventoryEnabled(false);
        tenantRepository.save(fixture.tenant);

        User user = createInventoryUser(fixture.tenant, fixture.pharmacyA);
        String token = loginAndExtractToken(user.getUsername() + "@" + fixture.tenant.getCode(), "pass123");

        mockMvc.perform(get("/inventory/alerts/summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Inventory module is disabled for this tenant."));
    }

    private User createInventoryUser(Tenant tenant, Pharmacy defaultPharmacy, Pharmacy... assignedPharmacies) {
        User user = new User();
        user.setTenant(tenant);
        user.setUsername("inv_" + UUID.randomUUID().toString().substring(0, 8));
        user.setPasswordHash(passwordEncoder.encode("pass123"));
        user.setRoles(Set.of(Role.INVENTORY));
        user.setDefaultPharmacy(defaultPharmacy);
        user.getAssignedPharmacies().add(defaultPharmacy);
        for (Pharmacy pharmacy : assignedPharmacies) {
            user.getAssignedPharmacies().add(pharmacy);
        }
        return userRepository.save(user);
    }

    private String loginAndExtractToken(String username, String password) throws Exception {
        String payload = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.path("token").asText();
    }

    private TestFixture createFixture() {
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Tenant tenant = new Tenant();
        tenant.setCode("ALERT" + suffix);
        tenant.setName("Alert Tenant " + suffix);
        tenant = tenantRepository.save(tenant);

        Pharmacy pharmacyA = new Pharmacy();
        pharmacyA.setTenant(tenant);
        pharmacyA.setCode("A" + suffix.substring(0, 3));
        pharmacyA.setName("Main Branch");
        pharmacyA = pharmacyRepository.save(pharmacyA);

        Pharmacy pharmacyB = new Pharmacy();
        pharmacyB.setTenant(tenant);
        pharmacyB.setCode("B" + suffix.substring(0, 3));
        pharmacyB.setName("Branch B");
        pharmacyB = pharmacyRepository.save(pharmacyB);

        tenant.setDefaultPharmacy(pharmacyA);
        tenantRepository.save(tenant);

        saveMedicine(tenant, pharmacyA, "A-LOW", 5, 50);
        saveMedicine(tenant, pharmacyA, "A-EXP", 25, 3);
        saveMedicine(tenant, pharmacyA, "A-SAFE", 40, 120);

        saveMedicine(tenant, pharmacyB, "B-LOW-EXP", 1, 2);
        saveMedicine(tenant, pharmacyB, "B-SAFE", 60, 180);

        return new TestFixture(tenant, pharmacyA, pharmacyB);
    }

    private void saveMedicine(Tenant tenant, Pharmacy pharmacy, String batchNumber, int quantity, int expiryInDays) {
        Medicine medicine = new Medicine();
        medicine.setTenant(tenant);
        medicine.setPharmacy(pharmacy);
        medicine.setName("Medicine-" + batchNumber);
        medicine.setBatchNumber(batchNumber + "-" + UUID.randomUUID().toString().substring(0, 4));
        medicine.setExpiryDate(LocalDate.now().plusDays(expiryInDays));
        medicine.setSupplier("Supplier");
        medicine.setUnitType("tablet");
        medicine.setPurchasePrice(new BigDecimal("10.00"));
        medicine.setSellingPrice(new BigDecimal("15.00"));
        medicine.setQuantity(quantity);
        medicineRepository.save(medicine);
    }

    private record TestFixture(Tenant tenant, Pharmacy pharmacyA, Pharmacy pharmacyB) {
    }
}


