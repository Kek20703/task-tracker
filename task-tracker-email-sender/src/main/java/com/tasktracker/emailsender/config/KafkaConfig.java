package com.tasktracker.emailsender.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/** Топики Kafka и обработка сбоев отправки. */
@Configuration
public class KafkaConfig {

	/** Топик с заданиями на отправку письма. Сообщения пишет backend. */
	public static final String EMAIL_SENDING_TASKS = "EMAIL_SENDING_TASKS";

	/** Топик-отстойник: сюда попадают письма, которые не удалось отправить после всех попыток. */
	public static final String EMAIL_SENDING_TASKS_DEAD_LETTER = EMAIL_SENDING_TASKS + ".DLT";

	private static final int PARTITIONS = 1;

	private final long maxAttempts;
	private final long retryIntervalMs;

	public KafkaConfig(
			@Value("${email-sender.retry.max-attempts:3}") long maxAttempts,
			@Value("${email-sender.retry.interval-ms:2000}") long retryIntervalMs) {
		this.maxAttempts = maxAttempts;
		this.retryIntervalMs = retryIntervalMs;
	}

	@Bean
	public NewTopic emailSendingTasksTopic() {
		return TopicBuilder.name(EMAIL_SENDING_TASKS).partitions(PARTITIONS).replicas(1).build();
	}

	@Bean
	public NewTopic emailSendingTasksDeadLetterTopic() {
		return TopicBuilder.name(EMAIL_SENDING_TASKS_DEAD_LETTER).partitions(PARTITIONS).replicas(1).build();
	}

	/**
	 * Повторяет отправку до {@code maxAttempts} раз (включая первую попытку) с паузой,
	 * а затем перекладывает сообщение в топик-отстойник. Так одна «отравленная» запись
	 * не блокирует очередь для остальных писем.
	 */
	@Bean
	public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<Object, Object> kafkaTemplate) {
		DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
				kafkaTemplate,
				(record, exception) -> new TopicPartition(EMAIL_SENDING_TASKS_DEAD_LETTER, record.partition()));
		// FixedBackOff считает повторы после первой попытки, поэтому вычитаем её.
		return new DefaultErrorHandler(recoverer, new FixedBackOff(retryIntervalMs, Math.max(0, maxAttempts - 1)));
	}
}
