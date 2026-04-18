package lk.pharmacy.inventory.util;

import jakarta.persistence.EntityManager;
import lk.pharmacy.inventory.domain.User;
import lk.pharmacy.inventory.domain.Pharmacy;
import lk.pharmacy.inventory.domain.Role;
import lk.pharmacy.inventory.exception.ApiException;
import lk.pharmacy.inventory.repo.UserRepository;
import lk.pharmacy.inventory.security.TenantUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;
    private final EntityManager entityManager;

    public CurrentUserService(UserRepository userRepository, EntityManager entityManager) {
        this.userRepository = userRepository;
        this.entityManager = entityManager;
    }

    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new ApiException("Unauthorized");
        }

        User user;
        Object principal = auth.getPrincipal();
        if (principal instanceof TenantUserPrincipal tenantPrincipal) {
            if (tenantPrincipal.getTenantId() != null) {
                user = userRepository.findByUsernameAndTenant_IdWithPharmacies(tenantPrincipal.getUsername(), tenantPrincipal.getTenantId())
                        .orElseThrow(() -> new ApiException("User not found"));
            } else {
                user = userRepository.findByUsernameAndTenantIsNull(tenantPrincipal.getUsername())
                        .orElseThrow(() -> new ApiException("User not found"));
            }
        } else {
            user = userRepository.findByUsernameAndTenantIsNull(auth.getName())
                    .orElseThrow(() -> new ApiException("User not found"));
        }
        return user;
    }

    public User getCurrentTenantUser() {
        User user = getCurrentUser();
        if (user.getTenant() == null) {
            throw new ApiException("Tenant context is required");
        }
        return user;
    }

    public String getCurrentUsername() {
        return getCurrentUser().getUsername();
    }

    public Long getCurrentTenantId() {
        User user = getCurrentTenantUser();
        return user.getTenant().getId();
    }

    public Long getCurrentPharmacyId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof TenantUserPrincipal principal) {
            return principal.getPharmacyId();
        }
        return null;
    }

    public Pharmacy getCurrentPharmacy() {
        User user = getCurrentTenantUser();
        Long pharmacyId = getCurrentPharmacyId();
        if (pharmacyId == null) {
            throw new ApiException("Pharmacy selection is required");
        }

        Pharmacy pharmacy = entityManager.find(Pharmacy.class, pharmacyId);
        if (pharmacy == null || !pharmacy.getTenant().getId().equals(user.getTenant().getId())) {
            throw new ApiException("Invalid pharmacy context");
        }
        if (!pharmacy.isEnabled()) {
            throw new ApiException("Selected pharmacy is disabled");
        }
        if (!hasPharmacyAccess(user, pharmacyId)) {
            throw new ApiException("You do not have access to this pharmacy");
        }
        return pharmacy;
    }

    public boolean hasPharmacyAccess(User user, Long pharmacyId) {
        if (user == null || user.getTenant() == null || pharmacyId == null) {
            return false;
        }
        if (user.hasRole(Role.ADMIN)) {
            return true;
        }
        return user.getAssignedPharmacies().stream().anyMatch(pharmacy -> pharmacy.getId().equals(pharmacyId));
    }
}

