package com.tasktracker.backend.dto;

import com.tasktracker.backend.validation.MaxBytes;
import com.tasktracker.backend.validation.NoNulCharacters;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(

        @NotBlank(message = "Email is required")
        @Size(max = 254, message = "Email is too long")
        @NoNulCharacters(message = "Email must not contain NUL characters")
        String email,

        @NotBlank(message = "Password is required")
        @MaxBytes(value = 72, message = "Password is too long")
        String password
) {

    /** Пароль не должен попадать в логи, даже если запрос случайно залогируют целиком. */
    @Override
    public String toString() {
        return "LoginRequest[email=" + email + ", password=***]";
    }
}
