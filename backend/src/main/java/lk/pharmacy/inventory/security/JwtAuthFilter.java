package lk.pharmacy.inventory.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AppUserDetailsService userDetailsService;

    public JwtAuthFilter(JwtService jwtService, AppUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        String username;
        Long tenantId;
        Long pharmacyId;
        try {
            username = jwtService.extractUsername(token);
            tenantId = jwtService.extractTenantId(token);
            pharmacyId = jwtService.extractPharmacyId(token);
        } catch (Exception ex) {
            filterChain.doFilter(request, response);
            return;
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails;
                if (tenantId != null) {
                    userDetails = userDetailsService.loadUserByUsernameAndTenantId(username, tenantId);
                } else {
                    userDetails = userDetailsService.loadSuperAdminByUsername(username);
                }
                if (jwtService.isValid(token, userDetails)) {
                    UserDetails contextUserDetails = userDetails;
                    if (userDetails instanceof TenantUserPrincipal principal) {
                        contextUserDetails = principal.withPharmacyId(pharmacyId);
                    }
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(contextUserDetails, null, contextUserDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } catch (UsernameNotFoundException ex) {
                // Token references a user that no longer exists — skip authentication
            }
        }

        filterChain.doFilter(request, response);
    }
}

