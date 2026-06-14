package com.melina.notes.exception;

public class NoteNotInTrashException extends RuntimeException {
    public NoteNotInTrashException(Long noteId) {
        super("Notiz " + noteId + " kann nur aus dem Papierkorb dauerhaft gelöscht werden");
    }
}
