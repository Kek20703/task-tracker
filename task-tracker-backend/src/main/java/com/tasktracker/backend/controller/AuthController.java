package com.tasktracker.backend.controller;

import com.tasktracker.backend.dto.LoginRequest;
import com.tasktracker.backend.dto.UserResponse;
import com.tasktracker.backend.service.AuthService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @SecurityRequirements
    @Operation(summary = "Вход по email и паролю",
            description = "Токен выдаётся в заголовке ответа Authorization: Bearer <token>.")
    @PostMapping(path = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserResponse> loginJson(@Valid @RequestBody LoginRequest request) {
        return AuthResponses.withToken(authService.login(request));
    }

    @SecurityRequirements
    @Operation(summary = "Вход по email и паролю",
            description = "Токен выдаётся в заголовке ответа Authorization: Bearer <token>.")
    @PostMapping(path = "/login", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<UserResponse> loginForm(@Valid @ModelAttribute LoginRequest request) {
        return AuthResponses.withToken(authService.login(request));
    }
}
