package com.melina.notes.exception;

public class TagLimitExceededException extends RuntimeException {
    public TagLimitExceededException(int maximum, int actual) {
        super("Eine Notiz darf höchstens " + maximum + " Tags enthalten; übergeben wurden " + actual);
    }
}
