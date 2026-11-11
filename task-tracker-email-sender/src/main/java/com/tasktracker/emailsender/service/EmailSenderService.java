package com.tasktracker.emailsender.service;

import com.tasktracker.emailsender.dto.EmailMessage;
import com.tasktracker.emailsender.exception.EmailSendingException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/** Отправляет письмо через SMTP. Адрес и порт сервера приходят из настроек Spring Mail. */
@Service
public class EmailSenderService {

	private static final Logger log = LoggerFactory.getLogger(EmailSenderService.class);

	private final JavaMailSender mailSender;
	private final String from;

	public EmailSenderService(JavaMailSender mailSender, @Value("${email-sender.from}") String from) {
		this.mailSender = mailSender;
		this.from = from;
	}

	/**
	 * Отправляет письмо. Явная кодировка UTF-8 нужна из-за кириллицы в теме и тексте.
	 * Любой сбой упаковывается в {@link EmailSendingException} — по нему Kafka повторит попытку.
	 */
	public void send(EmailMessage message) {
		try {
			MimeMessage mimeMessage = mailSender.createMimeMessage();
			MimeMessageHelper helper =
					new MimeMessageHelper(mimeMessage, false, StandardCharsets.UTF_8.name());
			helper.setFrom(from);
			helper.setTo(message.to());
			helper.setSubject(message.subject());
			helper.setText(message.text(), false);
			mailSender.send(mimeMessage);
			log.info("Письмо отправлено на {}", message.to());
		}
		catch (MessagingException | MailException e) {
			throw new EmailSendingException("Не удалось отправить письмо на " + message.to(), e);
		}
	}
}
