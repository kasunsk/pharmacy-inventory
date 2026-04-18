package lk.pharmacy.inventory.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(String username, Long tenantId, Long pharmacyId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .claim("tenantId", tenantId)
                .claim("pharmacyId", pharmacyId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(key)
                .compact();
    }

    public String generateToken(String username, Long tenantId) {
        return generateToken(username, tenantId, null);
    }

    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    public Long extractTenantId(String token) {
        Object tenantId = extractClaims(token).get("tenantId");
        return toLong(tenantId);
    }

    public Long extractPharmacyId(String token) {
        Object pharmacyId = extractClaims(token).get("pharmacyId");
        return toLong(pharmacyId);
    }

    private Long toLong(Object value) {
        if (value instanceof Integer intValue) {
            return intValue.longValue();
        }
        if (value instanceof Long longValue) {
            return longValue;
        }
        if (value instanceof Number numberValue) {
            return numberValue.longValue();
        }
        return value == null ? null : Long.parseLong(String.valueOf(value));
    }

    public boolean isValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isExpired(token);
    }

    private boolean isExpired(String token) {
        return extractClaims(token).getExpiration().before(new Date());
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

