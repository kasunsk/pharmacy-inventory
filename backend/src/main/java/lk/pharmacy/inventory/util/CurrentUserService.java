package lk.pharmacy.inventory.util;

import lk.pharmacy.inventory.domain.User;
import lk.pharmacy.inventory.exception.ApiException;
import lk.pharmacy.inventory.repo.UserRepository;
import lk.pharmacy.inventory.security.TenantUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
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
                user = userRepository.findByUsernameAndTenant_Id(tenantPrincipal.getUsername(), tenantPrincipal.getTenantId())
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
}

