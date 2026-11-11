package com.tasktracker.backend.validation;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Проверяет собственные ограничения валидации через настоящий Hibernate Validator. */
class CustomConstraintsTest {

    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();

    private final Validator validator = VALIDATOR_FACTORY.getValidator();

    @AfterAll
    static void closeValidatorFactory() {
        VALIDATOR_FACTORY.close();
    }

    // --- @MaxBytes: длина считается в байтах UTF-8, а не в символах ---

    static class MaxBytesSample {
        @MaxBytes(5)
        String value;

        MaxBytesSample(String value) {
            this.value = value;
        }
    }

    @Test
    void maxBytesAcceptsNull() {
        assertThat(validator.validate(new MaxBytesSample(null))).isEmpty();
    }

    @Test
    void maxBytesAcceptsValueOfExactlyTheLimit() {
        assertThat(validator.validate(new MaxBytesSample("12345"))).isEmpty();
    }

    @Test
    void maxBytesRejectsValueOverTheLimit() {
        assertThat(validator.validate(new MaxBytesSample("123456"))).hasSize(1);
    }

    @Test
    void maxBytesCountsCyrillicCharactersAsTwoBytes() {
        // «аб» — 4 байта, «абв» — 6 байт при лимите 5.
        assertThat(validator.validate(new MaxBytesSample("аб"))).isEmpty();
        assertThat(validator.validate(new MaxBytesSample("абв"))).hasSize(1);
    }

    // --- @NoNulCharacters: Postgres не хранит нулевой байт ---

    static class NoNulSample {
        @NoNulCharacters
        String value;

        NoNulSample(String value) {
            this.value = value;
        }
    }

    @Test
    void noNulAcceptsNullAndOrdinaryText() {
        assertThat(validator.validate(new NoNulSample(null))).isEmpty();
        assertThat(validator.validate(new NoNulSample("обычный текст"))).isEmpty();
    }

    @Test
    void noNulRejectsNulCharacter() {
        assertThat(validator.validate(new NoNulSample("a\u0000b"))).hasSize(1);
    }

    // --- @NullOrNotBlank: null допустим, пустая строка и одни пробелы — нет ---

    static class NullOrNotBlankSample {
        @NullOrNotBlank
        String value;

        NullOrNotBlankSample(String value) {
            this.value = value;
        }
    }

    @Test
    void nullOrNotBlankAcceptsNull() {
        assertThat(validator.validate(new NullOrNotBlankSample(null))).isEmpty();
    }

    @Test
    void nullOrNotBlankAcceptsText() {
        assertThat(validator.validate(new NullOrNotBlankSample("задача"))).isEmpty();
    }

    @Test
    void nullOrNotBlankRejectsEmptyAndWhitespaceOnly() {
        assertThat(validator.validate(new NullOrNotBlankSample(""))).hasSize(1);
        assertThat(validator.validate(new NullOrNotBlankSample("   "))).hasSize(1);
    }

    @Test
    void nullOrNotBlankRejectsUnicodeWhitespaceOnly() {
        // U+3000 (идеографический пробел) тоже считается пробелом — так же, как в String.isBlank().
        assertThat(validator.validate(new NullOrNotBlankSample("\u3000"))).hasSize(1);
    }
}
