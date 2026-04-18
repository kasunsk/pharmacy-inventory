package lk.pharmacy.inventory.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

public class TenantUserPrincipal implements UserDetails {

    private final Long userId;
    private final Long tenantId;
    private final String tenantCode;
    private final Long pharmacyId;
    private final String username;
    private final String password;
    private final boolean enabled;
    private final Collection<? extends GrantedAuthority> authorities;

    public TenantUserPrincipal(Long userId,
                               Long tenantId,
                               String tenantCode,
                               Long pharmacyId,
                               String username,
                               String password,
                               boolean enabled,
                               Collection<? extends GrantedAuthority> authorities) {
        this.userId = userId;
        this.tenantId = tenantId;
        this.tenantCode = tenantCode;
        this.pharmacyId = pharmacyId;
        this.username = username;
        this.password = password;
        this.enabled = enabled;
        this.authorities = authorities;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public Long getPharmacyId() {
        return pharmacyId;
    }

    public TenantUserPrincipal withPharmacyId(Long nextPharmacyId) {
        return new TenantUserPrincipal(userId, tenantId, tenantCode, nextPharmacyId, username, password, enabled, authorities);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
