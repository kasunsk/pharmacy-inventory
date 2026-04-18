package lk.pharmacy.inventory.security;

import lk.pharmacy.inventory.domain.User;
import lk.pharmacy.inventory.repo.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public AppUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        List<User> users = userRepository.findAllByUsername(username);
        if (users.size() != 1) {
            throw new UsernameNotFoundException("User not found or tenant context required");
        }
        return toPrincipal(users.get(0));
    }

    public TenantUserPrincipal loadUserByUsernameAndTenantId(String username, Long tenantId) {
        User user = userRepository.findByUsernameAndTenant_Id(username, tenantId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return toPrincipal(user);
    }

    public TenantUserPrincipal loadUserByUsernameAndTenantCode(String username, String tenantCode) {
        User user = userRepository.findByUsernameAndTenant_CodeIgnoreCase(username, tenantCode)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return toPrincipal(user);
    }

    public TenantUserPrincipal loadSuperAdminByUsername(String username) {
        User user = userRepository.findByUsernameAndTenantIsNull(username)
                .orElseThrow(() -> new UsernameNotFoundException("Super admin not found"));
        return toPrincipal(user);
    }

    private TenantUserPrincipal toPrincipal(User user) {
        return new TenantUserPrincipal(
                user.getId(),
                user.getTenant() == null ? null : user.getTenant().getId(),
                // Tenant code is not used in auth checks; avoid lazy-loading tenant proxy in filter path.
                null,
                null,
                user.getUsername(),
                user.getPasswordHash(),
                user.isEnabled(),
                user.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                        .toList()
        );
    }
}

