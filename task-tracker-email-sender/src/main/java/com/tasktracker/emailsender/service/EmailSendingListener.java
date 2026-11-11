package com.tasktracker.emailsender.service;

import com.tasktracker.emailsender.config.KafkaConfig;
import com.tasktracker.emailsender.dto.EmailMessage;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Точка входа сервиса: читает задания на отправку из {@code EMAIL_SENDING_TASKS}.
 * Ошибку отправки не глушит — её обрабатывает {@link KafkaConfig}: повторы, затем топик-отстойник.
 */
@Component
public class EmailSendingListener {

	private final EmailSenderService emailSenderService;

	public EmailSendingListener(EmailSenderService emailSenderService) {
		this.emailSenderService = emailSenderService;
	}

	@KafkaListener(topics = KafkaConfig.EMAIL_SENDING_TASKS)
	public void onEmailTask(EmailMessage message) {
		emailSenderService.send(message);
	}
}
