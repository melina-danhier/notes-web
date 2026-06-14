package com.melina.notes.exception;

public class InvalidNoteStateException extends RuntimeException {
    public InvalidNoteStateException(Long noteId, String message) {
        super("Notiz " + noteId + ": " + message);
    }
}
