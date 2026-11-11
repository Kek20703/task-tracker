package com.tasktracker.backend.controller;

import com.tasktracker.backend.dto.AuthResult;
import com.tasktracker.backend.dto.CurrentUser;
import com.tasktracker.backend.dto.UserResponse;
import com.tasktracker.backend.exception.AccountNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrentUsersTest {

    private static final UUID USER_ID = UUID.fromString("018f6d2e-1b7f-7cc1-9f31-3f6b8a2e4c5d");

    @Test
    void returnsIdOfCurrentUser() {
        assertThat(CurrentUsers.requireId(new CurrentUser(USER_ID, "user@example.com"))).isEqualTo(USER_ID);
    }

    @Test
    void missingPrincipalMeansNoAccount() {
        // Запрос авторизован не нашим JWT-фильтром — пользователя приложения за ним нет.
        assertThatThrownBy(() -> CurrentUsers.requireId(null)).isInstanceOf(AccountNotFoundException.class);
    }
}
