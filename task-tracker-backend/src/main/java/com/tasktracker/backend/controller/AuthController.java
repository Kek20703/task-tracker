package com.tasktracker.backend.controller;

import com.tasktracker.backend.dto.LoginRequest;
import com.tasktracker.backend.dto.UserResponse;
import com.tasktracker.backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
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
    @Operation(summary = "Вход по email и паролю", description = ApiDocs.FORM_ALSO_ACCEPTED)
    @ApiDocs.TokenInResponseHeader
    @PostMapping(path = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserResponse> loginJson(@Valid @RequestBody LoginRequest request) {
        return AuthResponses.withToken(authService.login(request));
    }

    /** То же для формы. В документации скрыт: springdoc слил бы его с JSON-методом в одну искажённую операцию. */
    @Operation(hidden = true)
    @PostMapping(path = "/login", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<UserResponse> loginForm(@Valid @ModelAttribute LoginRequest request) {
        return AuthResponses.withToken(authService.login(request));
    }
}
