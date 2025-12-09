package org.volumteerhub.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.volumteerhub.dto.ErrorResponse;

@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles ResourceNotFoundException and returns HTTP 404 NOT FOUND.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {

        HttpStatus status = HttpStatus.NOT_FOUND;

        ErrorResponse errorResponse = ErrorResponse.build(
                status,
                ex.getMessage(),
                request.getDescription(false).replace("uri=", "") // Extracts the request path
        );

        return new ResponseEntity<>(errorResponse, status);
    }

    /**
     * Handles UnauthorizedAccessException (for ownership/admin checks) and returns HTTP 403 FORBIDDEN.
     */
    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedAccessException(
            UnauthorizedAccessException ex, WebRequest request) {

        HttpStatus status = HttpStatus.FORBIDDEN;

        ErrorResponse errorResponse = ErrorResponse.build(
                status,
                ex.getMessage(),
                request.getDescription(false).replace("uri=", "") // Extracts the request path
        );

        return new ResponseEntity<>(errorResponse, status);
    }

//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<ErrorResponse> handleAllExceptions(Exception ex, WebRequest request) {
//        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
//
//        ErrorResponse errorResponse = ErrorResponse.build(
//                status,
//                "An unexpected error occurred: " + ex.getMessage(),
//                request.getDescription(false).replace("uri=", "")
//        );
//
//        return new ResponseEntity<>(errorResponse, status);
//    }
}
