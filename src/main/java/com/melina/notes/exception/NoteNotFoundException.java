package com.melina.notes.exception;

public class NoteNotFoundException extends RuntimeException {
    public NoteNotFoundException(String mes) {
        super(mes);
    }
}
