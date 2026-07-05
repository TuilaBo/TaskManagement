package com.example.TaskManagement.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    private static final Pattern HAS_LETTER = Pattern.compile("[a-zA-Z]");
    private static final Pattern HAS_DIGIT = Pattern.compile("[0-9]");

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.isBlank()) {
            return false;
        }
        boolean hasLetter = HAS_LETTER.matcher(password).find();
        boolean hasDigit = HAS_DIGIT.matcher(password).find();
        boolean hasMinLength = password.length() >= 8;
        return hasLetter && hasDigit && hasMinLength;
    }
}
