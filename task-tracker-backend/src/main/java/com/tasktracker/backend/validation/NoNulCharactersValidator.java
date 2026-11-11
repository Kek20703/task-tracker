package com.tasktracker.backend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NoNulCharactersValidator implements ConstraintValidator<NoNulCharacters, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || value.indexOf('\u0000') < 0;
    }
}
