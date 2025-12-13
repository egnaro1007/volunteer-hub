package org.volumteerhub.common.exception;

public class BadRequestException extends RuntimeException {
    /**
     * Constructs an BadRequestException with the specified detail message.
     * @param message the detail message.
     */
    public BadRequestException(String message) {
        super(message);
    }

    /**
     * Constructs an UnauthenticatedException with the specified detail message and cause.
     * @param message the detail BadRequestException.
     * @param cause the cause.
     */
    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
