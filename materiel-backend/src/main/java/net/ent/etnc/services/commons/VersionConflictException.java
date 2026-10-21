package net.ent.etnc.services.commons;

/**
 * Reports an optimistic concurrency conflict.
 */
public class VersionConflictException extends RuntimeException {

    /**
     * Creates an exception.
     *
     * @param message user-facing message
     */
    public VersionConflictException(final String message) {
        super(message);
    }
}
