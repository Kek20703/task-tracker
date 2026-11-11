package com.tasktracker.backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Для необязательных полей частичного обновления: поле можно не передавать (null),
 * но если оно передано, то не может состоять из одних пробелов. Пробелы определяются так же,
 * как в {@link String#isBlank()} и {@link String#strip()}, включая Unicode-пробелы.
 */
@Documented
@Constraint(validatedBy = NullOrNotBlankValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface NullOrNotBlank {

    String message() default "Value must not be blank";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
