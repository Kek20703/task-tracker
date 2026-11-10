package com.tasktracker.backend.dto;

/** То немногое, что нужно Spring Security для проверки пароля: email и хеш. */
public record UserCredentials(String email, String passwordHash) {

    /** Хеш пароля не должен попадать в логи. */
    @Override
    public String toString() {
        return "UserCredentials[email=" + email + ", passwordHash=***]";
    }
}
