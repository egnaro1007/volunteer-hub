package org.volumteerhub.common.exception;

/**
 * Exception thrown when a requested resource (like an Entity or DTO)
 * cannot be found in the system (e.g., in the database).
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Constructs a ResourceNotFoundException with the specified detail message.
     * @param message the detail message.
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructs a ResourceNotFoundException with the specified detail message and cause.
     * @param message the detail message.
     * @param cause the cause.
     */
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
