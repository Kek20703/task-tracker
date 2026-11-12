package com.tasktracker.backend.service;

import com.tasktracker.backend.dto.RegisterRequest;
import com.tasktracker.backend.entity.User;
import com.tasktracker.backend.exception.EmailAlreadyTakenException;
import com.tasktracker.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final UserService userService = new UserService(userRepository, passwordEncoder);

    @Test
    void reportsTakenEmailWhenConcurrentRegistrationWinsTheRace() {
        // Проверка existsByEmail прошла, но параллельный запрос успел вставить запись первым.
        when(userRepository.existsByEmail("my@email.com")).thenReturn(false);
        when(passwordEncoder.encode("secret-password")).thenReturn("hash");
        when(userRepository.save(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> userService.register(new RegisterRequest("my@email.com", "secret-password")))
                .isInstanceOf(EmailAlreadyTakenException.class);
    }

    @Test
    void requestToStringDoesNotExposePassword() {
        assertThat(new RegisterRequest("my@email.com", "secret-password").toString())
                .contains("my@email.com")
                .doesNotContain("secret-password");
    }
}
