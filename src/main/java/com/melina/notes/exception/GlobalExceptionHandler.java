package com.melina.notes.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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

    @ExceptionHandler(UserNoteMismatchException.class)
    public ResponseEntity<ErrorResponse> handleUserNoteMismatch(UserNoteMismatchException ex) {
        log.warn("Note access denied: {}", ex.getMessage());
        return responseEntityWithErrorResponse(HttpStatus.FORBIDDEN, ex);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        log.warn("Registration conflict: {}", ex.getMessage());
        return responseEntityWithErrorResponse(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler({
            InvalidNoteStateException.class,
            NoteDeletedException.class,
            NoteNotInTrashException.class
    })
    public ResponseEntity<ErrorResponse> handleNoteConflict(RuntimeException ex) {
        log.warn("Invalid note state: {}", ex.getMessage());
        return responseEntityWithErrorResponse(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler(TagLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleTagLimitExceeded(TagLimitExceededException ex) {
        log.warn("Tag limit exceeded: {}", ex.getMessage());
        return responseEntityWithErrorResponse(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler({InvalidRequestBodyException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ErrorResponse> handleInvalidRequestBody(Exception ex) {
        log.warn("Invalid request body: {}", ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST)
                .message("Ungültiger Request-Body")
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

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

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception ex) {
        log.warn("Bad Request: {}", ex.getMessage());
        return responseEntityWithErrorResponse(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleConflict(IllegalStateException ex) {
        log.warn("Conflict: {}", ex.getMessage());
        return responseEntityWithErrorResponse(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return responseEntityWithErrorResponse(HttpStatus.FORBIDDEN, ex);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.warn("Method not allowed: {}", ex.getMessage());
        return responseEntityWithErrorResponse(HttpStatus.METHOD_NOT_ALLOWED, ex);
    }

    @ExceptionHandler(RegistrationException.class)
    public ResponseEntity<ErrorResponse> handleRegistration(RegistrationException ex) {
        log.error("Registration failed", ex);
        return internalServerErrorResponse();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : "Unbekannter Fehler";
        log.error("Unerwarteter Fehler: {}", message, ex);

        return internalServerErrorResponse();
    }

    private ResponseEntity<ErrorResponse> internalServerErrorResponse() {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .message("Interner Serverfehler")
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
