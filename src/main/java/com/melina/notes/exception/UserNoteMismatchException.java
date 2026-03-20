package com.melina.notes.exception;

public class UserNoteMismatchException extends RuntimeException {
    public UserNoteMismatchException(Long userId, Long noteId) {
        super("User with id " + userId + " is not owner of Note with id " + noteId);
    }
}
