package com.melina.notes.validation;

import com.melina.notes.dto.SignupDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Objects;

public class PasswordsMatchValidator implements ConstraintValidator<PasswordsMatch, SignupDto> {

    @Override
    public boolean isValid(SignupDto signupDto, ConstraintValidatorContext context) {
        if (signupDto == null
                || signupDto.getPassword() == null
                || signupDto.getPassword().isBlank()
                || signupDto.getConfirmPassword() == null
                || signupDto.getConfirmPassword().isBlank()
                || Objects.equals(signupDto.getPassword(), signupDto.getConfirmPassword())) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("confirmPassword")
                .addConstraintViolation();
        return false;
    }
}
