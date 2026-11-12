package com.tasktracker.scheduler;

import com.tasktracker.scheduler.dto.EmailMessage;
import com.tasktracker.scheduler.dto.SummarizationRequest;
import com.tasktracker.scheduler.dto.SummarizationResponse;
import com.tasktracker.scheduler.dto.SummarizationTask;
import com.tasktracker.scheduler.service.EmailService;
import com.tasktracker.scheduler.service.RequestReplyService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.testcontainers.kafka.KafkaContainer;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import({TestcontainersConfiguration.class, SchedulerKafkaIT.EmailTopicConfig.class})
class SchedulerKafkaIT {

    @Autowired
    private RequestReplyService requestReplyService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private KafkaTemplate<Object, Object> kafkaTemplate;

    @Autowired
    private KafkaContainer kafkaContainer;

    @Value("${summarization.requests-topic}")
    private String requestsTopic;

    @Value("${summarization.responses-topic}")
    private String responsesTopic;

    @Value("${email.topic}")
    private String emailTopic;

    private final ExecutorService pool = Executors.newSingleThreadExecutor();

    @AfterEach
    void tearDown() {
        pool.shutdownNow();
    }

    @Test
    void requestReplyReturnsResponseWithSameRequestId() throws Exception {
        SummarizationRequest request = request();
        Future<?> responder = pool.submit(() -> {
            Map<String, Object> props = KafkaTestUtils.consumerProps(
                    kafkaContainer.getBootstrapServers(), "scheduler-rpc-responder", true);
            props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
            props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
            props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class);
            props.put(JacksonJsonDeserializer.VALUE_DEFAULT_TYPE, SummarizationRequest.class.getName());
            props.put(JacksonJsonDeserializer.USE_TYPE_INFO_HEADERS, false);
            props.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, SummarizationRequest.class.getPackageName());
            try (Consumer<String, SummarizationRequest> consumer =
                         new DefaultKafkaConsumerFactory<String, SummarizationRequest>(props).createConsumer()) {
                consumer.subscribe(List.of(requestsTopic));
                ConsumerRecords<String, SummarizationRequest> records =
                        KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10));
                SummarizationRequest received = records.iterator().next().value();
                kafkaTemplate.send(responsesTopic, received.requestId().toString(),
                        new SummarizationResponse(received.requestId(), "Отчёт из Kafka", true));
            }
        });

        SummarizationResponse response = requestReplyService.ask(request);

        assertThat(response.requestId()).isEqualTo(request.requestId());
        assertThat(response.report()).isEqualTo("Отчёт из Kafka");
        responder.get(10, TimeUnit.SECONDS);
    }

    @Test
    void emailIsJsonWithoutTypeHeaders() {
        EmailMessage message = new EmailMessage("user@example.com", "Тема", "Текст");
        emailService.send(message);

        Map<String, Object> props = KafkaTestUtils.consumerProps(
                kafkaContainer.getBootstrapServers(), "scheduler-email-check", true);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        try (Consumer<String, String> consumer =
                     new DefaultKafkaConsumerFactory<String, String>(props).createConsumer()) {
            consumer.subscribe(List.of(emailTopic));
            ConsumerRecords<String, String> records =
                    KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10));
            ConsumerRecord<String, String> record = records.iterator().next();
            assertThat(record.key()).isEqualTo("user@example.com");
            assertThat(record.headers().lastHeader("__TypeId__")).isNull();
            assertThat(record.value())
                    .contains("\"to\":\"user@example.com\"")
                    .contains("\"subject\":\"Тема\"")
                    .contains("\"text\":\"Текст\"");
        }
    }

    private static SummarizationRequest request() {
        Instant now = Instant.parse("2026-11-12T10:00:00Z");
        return new SummarizationRequest(
                UUID.randomUUID(), UUID.randomUUID(), now.minus(Duration.ofDays(1)), now,
                List.of(new SummarizationTask("Задача", "TODO", null, now)));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class EmailTopicConfig {

        @Bean
        org.apache.kafka.clients.admin.NewTopic emailTopic(@Value("${email.topic}") String topic) {
            return TopicBuilder.name(topic).partitions(1).replicas(1).build();
        }
    }
}
