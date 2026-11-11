package com.tasktracker.backend.exception;

public class EmailAlreadyTakenException extends RuntimeException {

    public EmailAlreadyTakenException() {
        super("This email is already taken");
    }
}
