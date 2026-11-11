package com.tasktracker.backend.controller;

import com.tasktracker.backend.dto.CurrentUser;
import com.tasktracker.backend.dto.RegisterRequest;
import com.tasktracker.backend.dto.UserResponse;
import com.tasktracker.backend.service.AuthService;
import com.tasktracker.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserController {

    private final AuthService authService;
    private final UserService userService;

    public UserController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @SecurityRequirements
    @Operation(summary = "Регистрация и сразу вход", description = ApiDocs.FORM_ALSO_ACCEPTED)
    @ApiDocs.TokenInResponseHeader
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserResponse> registerJson(@Valid @RequestBody RegisterRequest request) {
        return AuthResponses.withToken(authService.register(request));
    }

    /** То же для формы. В документации скрыт: springdoc слил бы его с JSON-методом в одну искажённую операцию. */
    @Operation(hidden = true)
    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<UserResponse> registerForm(@Valid @ModelAttribute RegisterRequest request) {
        return AuthResponses.withToken(authService.register(request));
    }

    @Operation(summary = "Текущий пользователь")
    @GetMapping
    public UserResponse currentUser(@AuthenticationPrincipal CurrentUser currentUser) {
        return userService.getById(CurrentUsers.requireId(currentUser));
    }
}
