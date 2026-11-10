package com.tasktracker.backend.service;

import com.tasktracker.backend.dto.LoginRequest;
import com.tasktracker.backend.exception.InvalidCredentialsException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private final UserService userService = mock(UserService.class);
    private final JwtService jwtService = mock(JwtService.class);
    private final AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
    private final AuthService authService = new AuthService(userService, jwtService, authenticationManager);

    private final LoginRequest request = new LoginRequest("my@email.com", "secret-password");

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
