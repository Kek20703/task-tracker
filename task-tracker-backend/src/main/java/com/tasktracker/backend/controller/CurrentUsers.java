package com.tasktracker.backend.controller;

import com.tasktracker.backend.dto.CurrentUser;
import com.tasktracker.backend.exception.AccountNotFoundException;

import java.util.UUID;

/** Достаёт id пользователя приложения из principal запроса. */
final class CurrentUsers {

    private CurrentUsers() {
    }

    static UUID requireId(CurrentUser currentUser) {
        if (currentUser == null) {
            // Запрос авторизован не нашим JWT-фильтром — пользователя приложения за ним нет.
            throw new AccountNotFoundException();
        }
        return currentUser.id();
    }
}
