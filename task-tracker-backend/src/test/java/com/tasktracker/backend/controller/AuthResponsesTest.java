package com.tasktracker.backend.controller;

import com.tasktracker.backend.dto.AuthResult;
import com.tasktracker.backend.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuthResponsesTest {

    @Test
    void tokenResponseCarriesUserAndBearerHeader() {
        UserResponse user = new UserResponse(UUID.randomUUID(), "user@example.com");

        ResponseEntity<UserResponse> response = AuthResponses.withToken(new AuthResult(user, "jwt-token"));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer jwt-token");
        assertThat(response.getBody()).isEqualTo(user);
    }
}
