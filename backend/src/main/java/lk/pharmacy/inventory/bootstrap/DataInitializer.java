package lk.pharmacy.inventory.bootstrap;

import lk.pharmacy.inventory.domain.Medicine;
import lk.pharmacy.inventory.domain.Role;
import lk.pharmacy.inventory.domain.User;
import lk.pharmacy.inventory.repo.MedicineRepository;
import lk.pharmacy.inventory.repo.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final MedicineRepository medicineRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           MedicineRepository medicineRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.medicineRepository = medicineRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!userRepository.existsByUsername("admin")) {
            User user = new User();
            user.setUsername("admin");
            user.setPasswordHash(passwordEncoder.encode("admin123"));
            user.setRole(Role.ADMIN);
            userRepository.save(user);
        }

        if (medicineRepository.count() == 0) {
            medicineRepository.saveAll(List.of(
                    medicine("Paracetamol 500mg", "SL-PARA-001", LocalDate.now().plusMonths(18), "Hemas Pharma", "18.00", "25.00", 220),
                    medicine("Amoxicillin 500mg", "SL-AMOX-002", LocalDate.now().plusMonths(14), "Cipla Lanka", "42.00", "60.00", 140),
                    medicine("Cetirizine 10mg", "SL-CETI-003", LocalDate.now().plusMonths(20), "State Pharma", "12.00", "20.00", 180),
                    medicine("Metformin 500mg", "SL-METF-004", LocalDate.now().plusMonths(16), "Sun Pharma", "24.00", "35.00", 160),
                    medicine("ORS Sachet", "SL-ORS-005", LocalDate.now().plusMonths(10), "GSK Sri Lanka", "28.00", "40.00", 95)
            ));
        }
    }

    private Medicine medicine(String name,
                              String batch,
                              LocalDate expiry,
                              String supplier,
                              String purchase,
                              String selling,
                              int quantity) {
        Medicine medicine = new Medicine();
        medicine.setName(name);
        medicine.setBatchNumber(batch);
        medicine.setExpiryDate(expiry);
        medicine.setSupplier(supplier);
        medicine.setPurchasePrice(new BigDecimal(purchase));
        medicine.setSellingPrice(new BigDecimal(selling));
        medicine.setQuantity(quantity);
        return medicine;
    }
}

