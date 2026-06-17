package com.melina.notes.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {
    private static final int MIN_LENGTH = 10;
    private static final int MAX_LENGTH = 72;

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.isBlank()) {
            return true;
        }

        return password.length() >= MIN_LENGTH
                && password.length() <= MAX_LENGTH;
    }
}
