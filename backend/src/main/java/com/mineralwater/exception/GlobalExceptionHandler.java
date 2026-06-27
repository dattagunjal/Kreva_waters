package com.mineralwater.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Converts all unhandled RuntimeExceptions into a consistent JSON body:
 *
 *   { "message": "Human-readable error text" }
 *
 * Without this, Spring returns an HTML error page on 500s, which the
 * Angular HttpClient can't parse — resulting in the "Failed to load data"
 * error swallowed by the generic error handler.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", ex.getMessage() != null
                        ? ex.getMessage()
                        : "An unexpected error occurred. Please try again."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        log.error("Internal server error: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Internal server error. Please try again later."));
    }
}
