package com.tasktracker.backend.security;

import com.tasktracker.backend.service.UserService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

/** Даёт Spring Security доступ к пользователям приложения: логин — это email. */
@Component
public class AppUserDetailsService implements UserDetailsService {

    static final String ROLE_USER = "USER";

    private final UserService userService;

    public AppUserDetailsService(UserService userService) {
        this.userService = userService;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        return userService.findCredentialsByEmail(email)
                .map(credentials -> User.withUsername(credentials.email())
                        .password(credentials.passwordHash())
                        .roles(ROLE_USER)
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
