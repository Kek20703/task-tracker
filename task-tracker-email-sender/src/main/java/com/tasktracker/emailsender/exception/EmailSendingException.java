package com.tasktracker.emailsender.exception;

/** Письмо не удалось отправить: SMTP недоступен или отказал в приёме. */
public class EmailSendingException extends RuntimeException {

	public EmailSendingException(String message, Throwable cause) {
		super(message, cause);
	}
}
