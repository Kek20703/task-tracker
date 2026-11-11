package com.tasktracker.backend.security;

import com.tasktracker.backend.dto.UserCredentials;
import com.tasktracker.backend.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AppUserDetailsServiceTest {

    private final UserService userService = mock(UserService.class);
    private final AppUserDetailsService userDetailsService = new AppUserDetailsService(userService);

    @Test
    void loadsUserByEmailWithUserRole() {
        when(userService.findCredentialsByEmail("user@example.com"))
                .thenReturn(Optional.of(new UserCredentials("user@example.com", "hash")));

        UserDetails details = userDetailsService.loadUserByUsername("user@example.com");

        assertThat(details.getUsername()).isEqualTo("user@example.com");
        assertThat(details.getPassword()).isEqualTo("hash");
        assertThat(details.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_USER");
    }

    @Test
    void unknownEmailIsReportedAsMissingUser() {
        when(userService.findCredentialsByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("missing@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
