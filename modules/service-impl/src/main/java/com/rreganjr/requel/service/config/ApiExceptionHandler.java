package com.rreganjr.requel.service.config;

import com.rreganjr.platform.command.AuthorizationException;
import com.rreganjr.requel.service.api.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for the API layer. Maps domain exceptions
 * to appropriate HTTP status codes and error response bodies.
 */
@RestControllerAdvice(basePackages = "com.rreganjr.requel.service")
public class ApiExceptionHandler {

    @ExceptionHandler(AuthorizationException.class)
    public ResponseEntity<ErrorResponse> handleAuthorization(AuthorizationException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of("FORBIDDEN", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of("BAD_REQUEST", e.getMessage()));
    }
}
