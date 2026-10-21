package net.ent.etnc.security.exceptions;

/**
 * Raised when a revoked refresh token is presented again.
 */
public class ReusedRefreshTokenException extends RuntimeException {
    /**
     * Creates the exception with a stable message.
     */
    public ReusedRefreshTokenException() {
        super("Refresh token réutilisé.");
    }
}
