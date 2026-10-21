package net.ent.etnc.services.impl;

import lombok.extern.slf4j.Slf4j;
import net.ent.etnc.models.RefreshToken;
import net.ent.etnc.models.User;
import net.ent.etnc.repositories.RefreshTokenRepository;
import net.ent.etnc.security.RefreshTokenProperties;
import net.ent.etnc.security.exceptions.ExpiredRefreshTokenException;
import net.ent.etnc.security.exceptions.InvalidRefreshTokenException;
import net.ent.etnc.security.exceptions.ReusedRefreshTokenException;
import net.ent.etnc.services.RefreshTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Transactional implementation of refresh-token creation, rotation and revocation.
 */
@Slf4j
@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {
    /**
     * Number of random bytes in every raw refresh token.
     */
    private static final int TOKEN_BYTES = 32;
    /**
     * Refresh token repository.
     */
    private final RefreshTokenRepository repository;
    /**
     * Refresh token settings.
     */
    private final RefreshTokenProperties properties;
    /**
     * Cryptographically secure random source.
     */
    private final SecureRandom secureRandom;
    /**
     * Injectable clock for deterministic tests.
     */
    private final Clock clock;

    /**
     * Creates the service.
     */
    @Autowired
    public RefreshTokenServiceImpl(RefreshTokenRepository repository, RefreshTokenProperties properties,
                                   SecureRandom secureRandom, Clock clock) {
        this.repository = repository;
        this.properties = properties;
        this.secureRandom = secureRandom;
        this.clock = clock;
    }

    /**
     * Creates a new token family and returns only the raw value for the HttpOnly cookie.
     */
    @Override
    @Transactional
    public String createToken(User user, String ipAddress, String userAgent) {
        log.debug("Création d'un refresh token pour l'utilisateur id={}, ip={}", user.getId(), ipAddress);
        return createToken(user, UUID.randomUUID().toString(), ipAddress, userAgent).rawToken();
    }

    /**
     * Rotates a valid token in one transaction and revokes token families on reuse.
     */
    @Override
    @Transactional(noRollbackFor = {ExpiredRefreshTokenException.class, ReusedRefreshTokenException.class})
    public RotateResult rotate(String rawToken, String ipAddress, String userAgent) {
        if (rawToken == null || rawToken.isBlank()) {
            log.warn("Tentative de rotation avec un refresh token vide");
            throw new InvalidRefreshTokenException();
        }
        Instant now = clock.instant();
        RefreshToken current = repository.findByTokenHashForUpdate(hash(rawToken))
                .orElseThrow(() -> {
                    log.warn("Tentative de rotation : refresh token inconnu");
                    return new InvalidRefreshTokenException();
                });

        if (current.isRevoked()) {
            log.error("ALERTE SÉCURITÉ : Réutilisation détectée d'un refresh token déjà révoqué (familyId={}, user={}) ! Révocation de la famille.",
                    current.getFamilyId(), current.getUser().getUsername());
            repository.revokeActiveByFamilyId(current.getFamilyId(), now);
            throw new ReusedRefreshTokenException();
        }

        if (current.isExpiredAt(now)) {
            log.warn("Tentative de rotation avec un refresh token expiré (familyId={}, user={})",
                    current.getFamilyId(), current.getUser().getUsername());
            current.revoke(now, null);
            throw new ExpiredRefreshTokenException();
        }

        GeneratedToken next = createToken(current.getUser(), current.getFamilyId(), ipAddress, userAgent);
        current.revoke(now, next.tokenHash());
        log.info("Rotation réussie du refresh token pour l'utilisateur '{}' (familyId={})",
                current.getUser().getUsername(), current.getFamilyId());
        return new RotateResult(current.getUser(), next.rawToken());
    }

    /**
     * Revokes a token idempotently for logout.
     */
    @Override
    @Transactional
    public void revokeToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return;
        repository.findByTokenHashForUpdate(hash(rawToken))
                .ifPresent(token -> {
                    log.info("Révocation du refresh token lors de la déconnexion (familyId={}, user={})",
                            token.getFamilyId(), token.getUser().getUsername());
                    token.revoke(clock.instant(), null);
                });
    }

    /**
     * Persists a generated token hash and returns the raw value plus hash.
     */
    private GeneratedToken createToken(User user, String familyId, String ipAddress, String userAgent) {
        Instant now = clock.instant();
        String rawToken = generateRawToken();
        String tokenHash = hash(rawToken);
        repository.save(new RefreshToken(tokenHash, familyId, user, now, now.plus(properties.getExpiration()),
                truncate(ipAddress, 64), truncate(userAgent, 512)));
        return new GeneratedToken(rawToken, tokenHash);
    }

    /**
     * Generates a URL-safe random refresh token.
     */
    private String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Computes a SHA-256 hexadecimal hash for storage.
     */
    private String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponible", exception);
        }
    }

    /**
     * Truncates audit metadata to fit database columns.
     */
    private String truncate(String value, int maxLength) {
        return value == null || value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    /**
     * Internal generated token pair.
     */
    private record GeneratedToken(String rawToken, String tokenHash) {
    }
}
