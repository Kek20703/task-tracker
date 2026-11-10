package com.tasktracker.backend.exception;

/** Неверная пара email/пароль. Сообщение одно и то же, чтобы нельзя было узнать, какой email зарегистрирован. */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
