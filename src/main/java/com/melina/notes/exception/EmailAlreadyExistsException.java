package com.melina.notes.exception;

public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException(String email) {
        super("Die E-Mail-Adresse " + email + " ist bereits registriert");
    }
}
