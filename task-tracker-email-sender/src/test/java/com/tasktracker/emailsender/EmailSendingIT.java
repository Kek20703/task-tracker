package com.tasktracker.emailsender;

import com.tasktracker.emailsender.config.KafkaConfig;
import com.tasktracker.emailsender.dto.EmailMessage;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.kafka.KafkaContainer;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * Сквозная проверка сервиса на настоящем Kafka в Testcontainers: сообщение из топика
 * превращается в письмо, а то, что отправить не удалось, после всех попыток уходит в топик-отстойник.
 * SMTP подменён заглушкой — проверяется связка Kafka → письмо, а не конкретный почтовый сервер.
 */
@SpringBootTest(properties = {
		"email-sender.retry.max-attempts=3",
		"email-sender.retry.interval-ms=50"
})
@Import(TestcontainersConfiguration.class)
class EmailSendingIT {

	@Autowired
	private KafkaTemplate<Object, Object> kafkaTemplate;

	@Autowired
	private KafkaContainer kafkaContainer;

	@MockitoBean
	private JavaMailSender mailSender;

	@BeforeEach
	void createRealMimeMessages() {
		given(mailSender.createMimeMessage())
				.willAnswer(invocation -> new MimeMessage(Session.getInstance(new Properties())));
	}

	@Test
	void messageFromTopicIsSentAsEmail() throws Exception {
		EmailMessage message = new EmailMessage("user@example.com", "Тема письма", "Текст письма");

		kafkaTemplate.send(KafkaConfig.EMAIL_SENDING_TASKS, message);

		ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
		verify(mailSender, timeout(Duration.ofSeconds(15).toMillis())).send(captor.capture());

		MimeMessage sent = captor.getValue();
		assertThat(sent.getSubject()).isEqualTo("Тема письма");
		assertThat(sent.getAllRecipients()).extracting(Object::toString).containsExactly("user@example.com");
		assertThat(sent.getContent()).isEqualTo("Текст письма");
	}

	@Test
	void messageThatFailsAllAttemptsGoesToDeadLetterTopic() {
		willThrow(new MailSendException("SMTP недоступен"))
				.given(mailSender).send(any(MimeMessage.class));

		EmailMessage message = new EmailMessage("user@example.com", "Тема письма", "Текст письма");
		kafkaTemplate.send(KafkaConfig.EMAIL_SENDING_TASKS, message);

		assertThat(readDeadLetterMessages()).containsExactly(message);
		verify(mailSender, times(3)).send(any(MimeMessage.class));
	}

	private List<EmailMessage> readDeadLetterMessages() {
		Map<String, Object> props = KafkaTestUtils.consumerProps(
				kafkaContainer.getBootstrapServers(), "dead-letter-check", true);
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class);
		props.put(JacksonJsonDeserializer.VALUE_DEFAULT_TYPE, EmailMessage.class.getName());
		props.put(JacksonJsonDeserializer.USE_TYPE_INFO_HEADERS, false);
		props.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, EmailMessage.class.getPackageName());

		try (Consumer<String, EmailMessage> consumer =
					 new DefaultKafkaConsumerFactory<String, EmailMessage>(props).createConsumer()) {
			consumer.subscribe(List.of(KafkaConfig.EMAIL_SENDING_TASKS_DEAD_LETTER));
			ConsumerRecords<String, EmailMessage> records =
					KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(15));
			return StreamSupport.stream(records.spliterator(), false)
					.map(record -> record.value())
					.toList();
		}
	}
}
