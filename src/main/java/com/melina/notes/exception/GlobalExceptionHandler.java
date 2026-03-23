package com.melina.notes.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.nio.file.AccessDeniedException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException ex) {
        if (ex.getMessage().contains("favicon.ico")) {
            return ResponseEntity.notFound().build();
        }
        log.warn("Resource not found: {}", ex.getMessage());
        return responseEntityWithErrorResponse(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(NoteNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoteNotFound(NoteNotFoundException ex) {
        log.warn("Note not found: {}", ex.getMessage());
        return responseEntityWithErrorResponse(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
        log.warn("User not found: {}", ex.getMessage());
        return responseEntityWithErrorResponse(HttpStatus.NOT_FOUND, ex);
    }

    // Validierungsfehler (400)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        log.warn("Validation failed: {}", ex.getMessage());

        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .collect(Collectors.toList());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST)
                .message("Validierungsfehler")
                .details(errors)
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    // Bad Request (400)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex) {
        log.warn("Bad Request: {}", ex.getMessage());
        return responseEntityWithErrorResponse(HttpStatus.BAD_REQUEST, ex);
    }

    // Conflict (409)
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleConflict(IllegalStateException ex) {
        log.warn("Conflict: {}", ex.getMessage());
        return responseEntityWithErrorResponse(HttpStatus.CONFLICT, ex);
    }

    // ✅ HTTP 403 Forbidden
    @ExceptionHandler({AccessDeniedException.class, HttpRequestMethodNotSupportedException.class})
    public ResponseEntity<ErrorResponse> handleForbidden(Exception ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return responseEntityWithErrorResponse(HttpStatus.FORBIDDEN, ex);
    }

    // FALLBACK
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : "Unbekannter Fehler";
        log.error("Unerwarteter Fehler: {}", message, ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .message("Interner Serverfehler")
                .details(Collections.singletonList(message))
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ErrorResponse> responseEntityWithErrorResponse(HttpStatus status, Exception ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(status)
                .message(ex.getMessage())
                .build();
        return new ResponseEntity<>(errorResponse, status);
    }

    private String formatFieldError(FieldError error) {
        return String.format("%s: %s", error.getField(), error.getDefaultMessage());
    }
}