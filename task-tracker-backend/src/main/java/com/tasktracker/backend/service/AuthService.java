package com.tasktracker.backend.service;

import com.tasktracker.backend.dto.AuthResult;
import com.tasktracker.backend.dto.LoginRequest;
import com.tasktracker.backend.dto.RegisterRequest;
import com.tasktracker.backend.dto.UserResponse;
import com.tasktracker.backend.exception.InvalidCredentialsException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserService userService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final WelcomeEmailService welcomeEmailService;

    public AuthService(UserService userService, JwtService jwtService,
                       AuthenticationManager authenticationManager, WelcomeEmailService welcomeEmailService) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.welcomeEmailService = welcomeEmailService;
    }

    /** Регистрирует пользователя, отправляет приветственное письмо и сразу выдаёт токен. */
    public AuthResult register(RegisterRequest request) {
        UserResponse user = userService.register(request);
        welcomeEmailService.send(user);
        return withToken(user);
    }

    public AuthResult login(LoginRequest request) {
        try {
            // Пароль сверяет Spring Security: он же выравнивает время ответа
            // для существующего и несуществующего email.
            authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.password()));
        } catch (BadCredentialsException e) {
            // Ловится только «неверный email или пароль». Остальное (например, недоступная база)
            // — не вина пользователя и должно дойти до обработчика как ошибка сервера.
            throw new InvalidCredentialsException();
        }
        UserResponse user = userService.findByEmail(request.email()).orElseThrow(InvalidCredentialsException::new);
        return withToken(user);
    }

    private AuthResult withToken(UserResponse user) {
        return new AuthResult(user, jwtService.issue(user.id(), user.email()));
    }
}
