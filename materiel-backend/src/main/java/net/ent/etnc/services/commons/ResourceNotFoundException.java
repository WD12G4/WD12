package net.ent.etnc.services.commons;

/**
 * Reports that a requested resource does not exist.
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Creates an exception.
     *
     * @param message user-facing message
     */
    public ResourceNotFoundException(final String message) {
        super(message);
    }
}
