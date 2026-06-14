package com.melina.notes.exception;

public class InvalidRequestBodyException extends RuntimeException {
    public InvalidRequestBodyException(String message) {
        super(message);
    }
}
