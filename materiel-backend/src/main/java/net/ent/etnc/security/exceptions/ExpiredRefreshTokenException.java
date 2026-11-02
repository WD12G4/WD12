package net.ent.etnc.security.exceptions;

/**
 * Raised when a refresh token exists but is expired.
 */
public class ExpiredRefreshTokenException extends RuntimeException {
    /**
     * Creates the exception with a stable message.
     */
    public ExpiredRefreshTokenException() {
        super("Refresh token expiré.");
    }
}
