package net.ent.etnc.services;


import net.ent.etnc.models.User;

/**
 * Business contract for HttpOnly refresh-token lifecycle operations.
 */
public interface RefreshTokenService {
    /**
     * Creates a new token family and returns the raw token for the cookie.
     */
    String createToken(User user, String ipAddress, String userAgent);

    /**
     * Rotates a valid raw token and returns the owner with the new raw token.
     */
    RotateResult rotate(String rawToken, String ipAddress, String userAgent);

    /**
     * Revokes a raw token idempotently, if it exists.
     */
    void revokeToken(String rawToken);

    /**
     * Result of a successful refresh token rotation.
     */
    record RotateResult(User user, String newRawToken) {
    }
}
