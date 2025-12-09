package org.volumteerhub.common.exception;

/**
 * Exception thrown when the authenticated user does not have the necessary
 * permissions or ownership to perform an action.
 */
public class UnauthorizedAccessException extends RuntimeException {

    /**
     * Constructs an UnauthorizedAccessException with the specified detail message.
     * @param message the detail message.
     */
    public UnauthorizedAccessException(String message) {
        super(message);
    }

    /**
     * Constructs an UnauthorizedAccessException with the specified detail message and cause.
     * @param message the detail message.
     * @param cause the cause.
     */
    public UnauthorizedAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
