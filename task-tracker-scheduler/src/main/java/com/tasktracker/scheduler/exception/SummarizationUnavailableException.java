package com.tasktracker.scheduler.exception;

/** Summarization-сервис не ответил за отведённое время. */
public class SummarizationUnavailableException extends RuntimeException {

    public SummarizationUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public SummarizationUnavailableException(String message) {
        super(message);
    }
}
