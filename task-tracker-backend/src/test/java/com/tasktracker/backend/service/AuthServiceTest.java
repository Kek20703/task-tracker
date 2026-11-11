package com.tasktracker.backend.service;

import com.tasktracker.backend.dto.LoginRequest;
import com.tasktracker.backend.dto.RegisterRequest;
import com.tasktracker.backend.dto.UserResponse;
import com.tasktracker.backend.exception.InvalidCredentialsException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private static final UUID USER_ID = UUID.fromString("018f6d2e-1b7f-7cc1-9f31-3f6b8a2e4c5d");

    private final UserService userService = mock(UserService.class);
    private final JwtService jwtService = mock(JwtService.class);
    private final AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
    private final WelcomeEmailService welcomeEmailService = mock(WelcomeEmailService.class);
    private final AuthService authService = new AuthService(
            userService, jwtService, authenticationManager, welcomeEmailService);

    private final LoginRequest request = new LoginRequest("my@email.com", "secret-password");

    @Test
    void registrationSendsWelcomeEmail() {
        RegisterRequest registerRequest = new RegisterRequest("my@email.com", "secret-password");
        UserResponse user = new UserResponse(USER_ID, "my@email.com");
        when(userService.register(registerRequest)).thenReturn(user);
        when(jwtService.issue(USER_ID, "my@email.com")).thenReturn("token");

        assertThat(authService.register(registerRequest).accessToken()).isEqualTo("token");
        verify(welcomeEmailService).send(user);
    }

    @Test
    void badCredentialsBecomeInvalidCredentials() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThatThrownBy(() -> authService.login(request)).isInstanceOf(InvalidCredentialsException.class);
        verifyNoInteractions(jwtService);
    }

    @Test
    void infrastructureFailureIsNotMaskedAsWrongPassword() {
        // Так DaoAuthenticationProvider заворачивает сбой UserDetailsService, например недоступную базу.
        when(authenticationManager.authenticate(any())).thenThrow(new InternalAuthenticationServiceException(
                "db is down", new DataAccessResourceFailureException("connection refused")));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InternalAuthenticationServiceException.class);
        verifyNoInteractions(jwtService);
    }

    @Test
    void loginRequestToStringDoesNotExposePassword() {
        assertThat(request.toString()).contains("my@email.com").doesNotContain("secret-password");
    }
}
