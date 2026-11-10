package com.tasktracker.backend.exception;

import org.springframework.security.core.AuthenticationException;

/** Токен действителен, но пользователя уже нет в базе. Для клиента это то же, что отсутствие авторизации. */
public class AccountNotFoundException extends AuthenticationException {

    public AccountNotFoundException() {
        super("Account no longer exists");
    }
}
