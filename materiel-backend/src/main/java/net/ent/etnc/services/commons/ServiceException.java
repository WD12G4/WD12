package net.ent.etnc.services.commons;

/**
 * Reports a business-rule or request-contract violation.
 */
public class ServiceException extends RuntimeException {

    /**
     * Creates an exception.
     *
     * @param message user-facing message
     */
    public ServiceException(final String message) {
        super(message);
    }

    /**
     * Creates an exception with a cause.
     *
     * @param message user-facing message
     * @param cause   root cause
     */
    public ServiceException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
