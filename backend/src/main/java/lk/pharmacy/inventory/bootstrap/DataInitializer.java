package lk.pharmacy.inventory.bootstrap;

import lk.pharmacy.inventory.domain.Medicine;
import lk.pharmacy.inventory.domain.Pharmacy;
import lk.pharmacy.inventory.domain.Role;
import lk.pharmacy.inventory.domain.Sale;
import lk.pharmacy.inventory.domain.Tenant;
import lk.pharmacy.inventory.domain.User;
import lk.pharmacy.inventory.repo.MedicineRepository;
import lk.pharmacy.inventory.repo.PharmacyRepository;
import lk.pharmacy.inventory.repo.SaleRepository;
import lk.pharmacy.inventory.repo.TenantRepository;
import lk.pharmacy.inventory.repo.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final MedicineRepository medicineRepository;
    private final SaleRepository saleRepository;
    private final TenantRepository tenantRepository;
    private final PharmacyRepository pharmacyRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           MedicineRepository medicineRepository,
                           SaleRepository saleRepository,
                           TenantRepository tenantRepository,
                           PharmacyRepository pharmacyRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.medicineRepository = medicineRepository;
        this.saleRepository = saleRepository;
        this.tenantRepository = tenantRepository;
        this.pharmacyRepository = pharmacyRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Tenant defaultTenant = tenantRepository.findByCode("DEFAULT")
                .orElseGet(() -> {
                    Tenant tenant = new Tenant();
                    tenant.setCode("DEFAULT");
                    tenant.setName("Default Pharmacy");
                    return tenantRepository.save(tenant);
                });

        Pharmacy defaultPharmacy = pharmacyRepository.findByTenant_IdAndCodeIgnoreCase(defaultTenant.getId(), "MAIN")
                .orElseGet(() -> {
                    Pharmacy pharmacy = new Pharmacy();
                    pharmacy.setTenant(defaultTenant);
                    pharmacy.setCode("MAIN");
                    pharmacy.setName("Main Pharmacy");
                    return pharmacyRepository.save(pharmacy);
                });

        if (defaultTenant.getDefaultPharmacy() == null) {
            defaultTenant.setDefaultPharmacy(defaultPharmacy);
            tenantRepository.save(defaultTenant);
        }


        // Backfill rows created before multi-tenancy was introduced.
        // SUPER_ADMIN users have null tenant intentionally — skip them.
        userRepository.findByTenantIsNull().stream()
                .filter(user -> !user.hasRole(Role.SUPER_ADMIN))
                .forEach(user -> user.setTenant(defaultTenant));
        medicineRepository.findByTenantIsNull().forEach(medicine -> {
            medicine.setTenant(defaultTenant);
            if (medicine.getPharmacy() == null) {
                medicine.setPharmacy(defaultPharmacy);
            }
        });
        saleRepository.findByTenantIsNull().forEach(sale -> assignSaleTenantAndPharmacy(sale, defaultTenant, defaultPharmacy));

        userRepository.findByTenant_Id(defaultTenant.getId()).forEach(user -> {
            if (!user.hasRole(Role.SUPER_ADMIN) && user.getAssignedPharmacies().isEmpty()) {
                user.getAssignedPharmacies().add(defaultPharmacy);
            }
            if (user.getDefaultPharmacy() == null && !user.hasRole(Role.SUPER_ADMIN)) {
                user.setDefaultPharmacy(defaultPharmacy);
            }
        });

        medicineRepository.findByTenant_Id(defaultTenant.getId()).forEach(medicine -> {
            if (medicine.getPharmacy() == null) {
                medicine.setPharmacy(defaultPharmacy);
            }
        });

        saleRepository.findByTenant_IdAndCreatedAtBetweenOrderByCreatedAtDesc(defaultTenant.getId(), java.time.Instant.EPOCH, java.time.Instant.now().plusSeconds(1))
                .forEach(sale -> {
                    if (sale.getPharmacy() == null) {
                        sale.setPharmacy(defaultPharmacy);
                    }
                });

        userRepository.flush();
        medicineRepository.flush();
        saleRepository.flush();

        // Use tenant-scoped lookup to avoid IncorrectResultSizeDataAccessException
        // when another tenant also has an "admin" user.
        userRepository.findByUsernameAndTenant_Id("admin", defaultTenant.getId()).ifPresentOrElse(existing -> {
            if (existing.getRoles() == null || existing.getRoles().isEmpty()) {
                existing.setRoles(Set.of(Role.ADMIN));
                userRepository.save(existing);
            }
        }, () -> {
            User user = new User();
            user.setTenant(defaultTenant);
            user.setUsername("admin");
            user.setPasswordHash(passwordEncoder.encode("admin123"));
            user.setRoles(Set.of(Role.ADMIN));
            user.setDefaultPharmacy(defaultPharmacy);
            user.getAssignedPharmacies().add(defaultPharmacy);
            userRepository.save(user);
        });

        // Seed super_admin (no tenant association)
        userRepository.findByUsernameAndTenantIsNull("super_admin").ifPresentOrElse(
                existing -> {
                    if (existing.getRoles() == null || !existing.hasRole(Role.SUPER_ADMIN)) {
                        existing.setRoles(Set.of(Role.SUPER_ADMIN));
                        userRepository.save(existing);
                    }
                },
                () -> {
                    User superAdmin = new User();
                    superAdmin.setUsername("super_admin");
                    superAdmin.setPasswordHash(passwordEncoder.encode("admin@123"));
                    superAdmin.setRoles(Set.of(Role.SUPER_ADMIN));
                    userRepository.save(superAdmin);
                }
        );

        if (medicineRepository.findByTenant_Id(defaultTenant.getId()).isEmpty()) {
            medicineRepository.saveAll(List.of(
                    medicine(defaultTenant, "Paracetamol 500mg", "SL-PARA-001", LocalDate.now().plusMonths(18), "Hemas Pharma", "tablet", "18.00", "25.00", 220),
                    medicine(defaultTenant, "Amoxicillin 500mg", "SL-AMOX-002", LocalDate.now().plusMonths(14), "Cipla Lanka", "capsule", "42.00", "60.00", 140),
                    medicine(defaultTenant, "Cetirizine 10mg", "SL-CETI-003", LocalDate.now().plusMonths(20), "State Pharma", "tablet", "12.00", "20.00", 180),
                    medicine(defaultTenant, "Metformin 500mg", "SL-METF-004", LocalDate.now().plusMonths(16), "Sun Pharma", "tablet", "24.00", "35.00", 160),
                    medicine(defaultTenant, "ORS Sachet", "SL-ORS-005", LocalDate.now().plusMonths(10), "GSK Sri Lanka", "sachet", "28.00", "40.00", 95)
            ));
        }
    }

    private void assignSaleTenantAndPharmacy(Sale sale, Tenant defaultTenant, Pharmacy defaultPharmacy) {
        if (sale.getTenant() != null) {
            if (sale.getPharmacy() == null) {
                sale.setPharmacy(defaultPharmacy);
            }
            return;
        }
        if (sale.getCreatedBy() != null && sale.getCreatedBy().getTenant() != null) {
            sale.setTenant(sale.getCreatedBy().getTenant());
            sale.setPharmacy(defaultPharmacy);
            return;
        }
        sale.setTenant(defaultTenant);
        sale.setPharmacy(defaultPharmacy);
    }

    private Medicine medicine(Tenant tenant,
                              String name,
                              String batch,
                              LocalDate expiry,
                              String supplier,
                              String unitType,
                              String purchase,
                              String selling,
                              int quantity) {
        Medicine medicine = new Medicine();
        medicine.setTenant(tenant);
        Pharmacy pharmacy = tenant.getDefaultPharmacy();
        if (pharmacy != null) {
            medicine.setPharmacy(pharmacy);
        }
        medicine.setName(name);
        medicine.setBatchNumber(batch);
        medicine.setExpiryDate(expiry);
        medicine.setSupplier(supplier);
        medicine.setUnitType(unitType);
        medicine.setPurchasePrice(new BigDecimal(purchase));
        medicine.setSellingPrice(new BigDecimal(selling));
        medicine.setQuantity(quantity);
        return medicine;
    }
}

