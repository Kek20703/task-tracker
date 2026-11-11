package com.tasktracker.emailsender.service;

import com.tasktracker.emailsender.dto.EmailMessage;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class EmailSendingListenerTest {

	private final EmailSenderService emailSenderService = mock(EmailSenderService.class);
	private final EmailSendingListener listener = new EmailSendingListener(emailSenderService);

	@Test
	void passesMessageToSender() {
		EmailMessage message = new EmailMessage("user@example.com", "Тема", "Текст");

		listener.onEmailTask(message);

		verify(emailSenderService).send(message);
		verifyNoMoreInteractions(emailSenderService);
	}
}
