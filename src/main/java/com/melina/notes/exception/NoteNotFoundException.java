package com.melina.notes.exception;

public class NoteNotFoundException extends RuntimeException {
    public NoteNotFoundException(Long noteId) {
        super("Note with id " + noteId + " not found");
    }
}
