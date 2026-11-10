package com.tasktracker.backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Ограничивает длину строки в байтах UTF-8, а не в символах.
 * Нужно для паролей: BCrypt принимает не больше 72 байт, а кириллический символ занимает два.
 */
@Documented
@Constraint(validatedBy = MaxBytesValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface MaxBytes {

    int value();

    String message() default "Value is too long";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
