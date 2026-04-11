package lk.pharmacy.inventory.files;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lk.pharmacy.inventory.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class SignedUrlService {

    private final SecretKey key;
    private final long expirationMs;

    public SignedUrlService(@Value("${app.file-sign.secret}") String secret,
                            @Value("${app.file-sign.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String createToken(Long fileId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(fileId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(key)
                .compact();
    }

    public void validateToken(Long fileId, String token) {
        try {
            String subject = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();
            if (!String.valueOf(fileId).equals(subject)) {
                throw new ApiException("Invalid signed URL token");
            }
        } catch (Exception e) {
            throw new ApiException("Invalid or expired signed URL token");
        }
    }
}

