package com.melina.notes.exception;

public class RegistrationException extends RuntimeException {
    public RegistrationException(Throwable cause) {
        super("Registrierung fehlgeschlagen. Bitte versuchen Sie es erneut", cause);
    }
}
