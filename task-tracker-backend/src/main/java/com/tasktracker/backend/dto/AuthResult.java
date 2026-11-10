package com.tasktracker.backend.dto;

/** Итог регистрации или входа: пользователь и выданный ему access token. */
public record AuthResult(UserResponse user, String accessToken) {

    /** Токен не должен попадать в логи. */
    @Override
    public String toString() {
        return "AuthResult[user=" + user + ", accessToken=***]";
    }
}
