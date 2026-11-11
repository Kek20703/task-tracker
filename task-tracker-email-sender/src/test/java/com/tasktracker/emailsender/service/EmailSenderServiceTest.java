package com.tasktracker.emailsender.service;

import com.tasktracker.emailsender.dto.EmailMessage;
import com.tasktracker.emailsender.exception.EmailSendingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/** Юнит-тесты сборки письма: SMTP подменён заглушкой, проверяется содержимое MimeMessage. */
class EmailSenderServiceTest {

	private static final String FROM = "no-reply@task-tracker.local";

	private final JavaMailSender mailSender = mock(JavaMailSender.class);
	private final EmailSenderService emailSenderService = new EmailSenderService(mailSender, FROM);

	private final EmailMessage message = new EmailMessage("user@example.com", "Тема письма", "Текст письма");

	@BeforeEach
	void createRealMimeMessages() {
		given(mailSender.createMimeMessage())
				.willAnswer(invocation -> new MimeMessage(Session.getInstance(new Properties())));
	}

	@Test
	void sendsLetterWithFromSubjectRecipientAndText() throws Exception {
		emailSenderService.send(message);

		ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
		verify(mailSender).send(captor.capture());

		MimeMessage sent = captor.getValue();
		assertThat(sent.getFrom()).extracting(Object::toString).containsExactly(FROM);
		assertThat(sent.getAllRecipients()).extracting(Object::toString).containsExactly("user@example.com");
		assertThat(sent.getSubject()).isEqualTo("Тема письма");
		assertThat(sent.getContent()).isEqualTo("Текст письма");
	}

	@Test
	void mailServerFailureIsReportedAsSendingFailure() {
		willThrow(new MailSendException("SMTP недоступен")).given(mailSender).send(any(MimeMessage.class));

		assertThatThrownBy(() -> emailSenderService.send(message))
				.isInstanceOf(EmailSendingException.class)
				.hasMessageContaining("user@example.com")
				.hasCauseInstanceOf(MailSendException.class);
	}

	@Test
	void failureToBuildLetterIsReportedAsSendingFailure() {
		given(mailSender.createMimeMessage()).willThrow(new MailParseException("не удалось собрать письмо"));

		assertThatThrownBy(() -> emailSenderService.send(message))
				.isInstanceOf(EmailSendingException.class)
				.hasCauseInstanceOf(MailParseException.class);
	}
}
