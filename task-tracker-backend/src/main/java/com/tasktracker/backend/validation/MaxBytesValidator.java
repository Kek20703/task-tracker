package com.tasktracker.backend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.nio.charset.StandardCharsets;

public class MaxBytesValidator implements ConstraintValidator<MaxBytes, String> {

    private int max;

    @Override
    public void initialize(MaxBytes constraint) {
        this.max = constraint.value();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // null проверяет @NotBlank, здесь он считается допустимым.
        return value == null || value.getBytes(StandardCharsets.UTF_8).length <= max;
    }
}
