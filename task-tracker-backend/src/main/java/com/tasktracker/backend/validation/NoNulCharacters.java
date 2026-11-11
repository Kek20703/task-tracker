package com.tasktracker.backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Запрещает символ U+0000. Postgres не хранит его в текстовых колонках и не принимает в параметрах
 * запросов: без этой проверки такая строка доходила бы до базы и превращалась в ошибку сервера.
 */
@Documented
@Constraint(validatedBy = NoNulCharactersValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface NoNulCharacters {

    String message() default "Value must not contain NUL characters";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
