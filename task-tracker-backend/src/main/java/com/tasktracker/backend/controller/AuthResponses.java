package com.tasktracker.backend.controller;

import com.tasktracker.backend.dto.AuthResult;
import com.tasktracker.backend.dto.UserResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

/** Общий вид ответа на регистрацию и вход: пользователь в теле, токен в заголовке Authorization. */
final class AuthResponses {

    private AuthResponses() {
    }

    static ResponseEntity<UserResponse> withToken(AuthResult result) {
        return ResponseEntity.ok()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + result.accessToken())
                .body(result.user());
    }
}
