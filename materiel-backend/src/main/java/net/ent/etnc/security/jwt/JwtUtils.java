package net.ent.etnc.security.jwt;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import net.ent.etnc.models.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

/**
 * Generates and validates signed JWT access tokens.
 */
@Component
public class JwtUtils {
    /**
     * HMAC secret injected from configuration.
     */
    @Value("${app.security.jwt.secret}")
    private String secret;
    /**
     * Access token lifetime in milliseconds.
     */
    @Value("${app.security.jwt.expiration:900000}")
    private long jwtExpirationMs;

    /**
     * Generates an access token for the authenticated principal.
     */
    public String generateJwtToken(Authentication authentication) {
        return generateJwtTokenForUser((User) authentication.getPrincipal());
    }

    /**
     * Generates an access token for a user.
     */
    public String generateJwtTokenForUser(User user) {
        return Jwts.builder()
                .subject(user.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .claim("role", user.getRole().name())
                .signWith(getSecretKey())
                .compact();
    }

    /**
     * Returns the subject when the token signature and expiration are valid.
     */
    public Optional<String> getUsernameIfValid(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        try {
            return Optional.ofNullable(Jwts.parser().verifyWith(getSecretKey()).build()
                    .parseSignedClaims(token).getPayload().getSubject());
        } catch (JwtException | IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    /**
     * Builds the HMAC key from the configured secret.
     */
    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
