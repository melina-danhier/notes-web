package com.melina.notes.exception;

public class NoteDeletedException extends RuntimeException {
    public NoteDeletedException(Long noteId, String action) {
        super("Gelöschte Notiz " + noteId + " kann nicht " + action + " werden");
    }
}
