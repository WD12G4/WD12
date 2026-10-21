package net.ent.etnc.security.exceptions;

/**
 * Raised when a refresh token is missing or unknown.
 */
public class InvalidRefreshTokenException extends RuntimeException {
    /**
     * Creates the exception with a stable message.
     */
    public InvalidRefreshTokenException() {
        super("Refresh token invalide.");
    }
}
