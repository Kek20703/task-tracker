package com.tasktracker.backend.dto;

import com.tasktracker.backend.validation.MaxBytes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(

        @NotBlank(message = "Email is required")
        @Size(max = 254, message = "Email is too long")
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
