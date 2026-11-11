package com.tasktracker.backend.dto;

import com.tasktracker.backend.validation.MaxBytes;
import com.tasktracker.backend.validation.NoNulCharacters;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Email is not valid")
        @Size(max = 254, message = "Email is too long")
        @NoNulCharacters(message = "Email must not contain NUL characters")
        String email,

        // BCrypt принимает не больше 72 байт, поэтому верхняя граница считается в байтах.
        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters long")
        @MaxBytes(value = 72, message = "Password is too long")
        String password
) {

    /** Пароль не должен попадать в логи, даже если запрос случайно залогируют целиком. */
    @Override
    public String toString() {
        return "RegisterRequest[email=" + email + ", password=***]";
    }
}
