package org.volumteerhub.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.multipart.MultipartException;
import org.volumteerhub.dto.ErrorResponse;

@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles handleMethodNotSupportedException and returns HTTP 405 METHOD NOT ALLOWED.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e, WebRequest request) {

        HttpStatus status = HttpStatus.METHOD_NOT_ALLOWED;

        String[] supportedMethods = e.getSupportedMethods();
        String allowedMethodsString = null;
        if (supportedMethods != null && supportedMethods.length > 0) {
            allowedMethodsString = String.format(" Allowed methods: [%s].", String.join(",", supportedMethods));
        }

        String detailMessage = String.format(
                "Request method '%s' not supported for this endpoint.%s",
                e.getMethod(),
                allowedMethodsString
        );

        ErrorResponse errorResponse = ErrorResponse.build(
                status,
                detailMessage,
                request.getDescription(false).replace("uri=", "")
        );

        return new ResponseEntity<>(errorResponse, status);
    }

    /**
     * Handles ResourceNotFoundException and returns HTTP 404 NOT FOUND.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException e, WebRequest request) {

        HttpStatus status = HttpStatus.NOT_FOUND;

        ErrorResponse errorResponse = ErrorResponse.build(
                status,
                e.getMessage(),
                request.getDescription(false).replace("uri=", "")
        );

        return new ResponseEntity<>(errorResponse, status);
    }

    /**
     * Handles UnauthorizedAccessException (for ownership/admin checks) and returns HTTP 403 FORBIDDEN.
     */
    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedAccessException(
            UnauthorizedAccessException e, WebRequest request) {

        HttpStatus status = HttpStatus.FORBIDDEN;

        ErrorResponse errorResponse = ErrorResponse.build(
                status,
                e.getMessage(),
                request.getDescription(false).replace("uri=", "")
        );

        return new ResponseEntity<>(errorResponse, status);
    }

    /**
     * Handles BadRequestException and returns HTTP 400 FORBIDDEN.
     */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequestException(
            BadRequestException e, WebRequest request) {

        HttpStatus status = HttpStatus.BAD_REQUEST;

        ErrorResponse errorResponse = ErrorResponse.build(
                status,
                e.getMessage(),
                request.getDescription(false).replace("uri=", "")
        );

        return new ResponseEntity<>(errorResponse, status);
    }

    /**
     * Handles MultipartException and returns HTTP 400 BAD REQUEST.
     */
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ErrorResponse> handleMultipartException(
            MultipartException e, WebRequest request) {

        HttpStatus status = HttpStatus.BAD_REQUEST;

        ErrorResponse errorResponse = ErrorResponse.build(
                status,
                "Invalid multipart request: " + e.getMessage(),
                request.getDescription(false).replace("uri=", "")
        );

        return new ResponseEntity<>(errorResponse, status);
    }

//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<ErrorResponse> handleAllExceptions(Exception e, WebRequest request) {
//        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
//
//        ErrorResponse errorResponse = ErrorResponse.build(
//                status,
//                "An unexpected error occurred: " + e.getMessage(),
//                request.getDescription(false).replace("uri=", "")
//        );
//
//        return new ResponseEntity<>(errorResponse, status);
//    }
}
