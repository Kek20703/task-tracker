package com.tasktracker.summarization;

import com.tasktracker.summarization.dto.SummarizationRequest;
import com.tasktracker.summarization.dto.SummarizationResponse;
import com.tasktracker.summarization.dto.SummarizationTask;
import com.tasktracker.summarization.entity.SummarizationRecord;
import com.tasktracker.summarization.repository.SummarizationRecordRepository;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SummarizationIT {

    @Autowired
    private KafkaTemplate<Object, Object> kafkaTemplate;

    @Autowired
    private KafkaContainer kafkaContainer;

    @Autowired
    private SummarizationRecordRepository repository;

    @Value("${summarization.requests-topic}")
    private String requestsTopic;

    @Value("${summarization.responses-topic}")
    private String responsesTopic;

    @BeforeEach
    void cleanUp() {
        repository.deleteAll();
    }

    @Test
    void consumesRequestAndReturnsResponse() {
        SummarizationRequest request = request();
        kafkaTemplate.send(requestsTopic, request.requestId().toString(), request);

        ConsumerRecord<String, SummarizationResponse> record = readResponse();

        assertThat(record.key()).isEqualTo(request.requestId().toString());
        assertThat(record.value().requestId()).isEqualTo(request.requestId());
        assertThat(record.value().usedLlm()).isFalse();
        assertThat(record.value().report()).contains("Всего задач: 2");

        SummarizationRecord saved = repository.findByRequestId(request.requestId()).orElseThrow();
        assertThat(record.value().report()).isEqualTo(saved.getReport());
        assertThat(saved.getTaskCount()).isEqualTo(2);
    }

    private ConsumerRecord<String, SummarizationResponse> readResponse() {
        Map<String, Object> props = KafkaTestUtils.consumerProps(
                kafkaContainer.getBootstrapServers(), "summarization-response-check", true);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class);
        props.put(JacksonJsonDeserializer.VALUE_DEFAULT_TYPE, SummarizationResponse.class.getName());
        props.put(JacksonJsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        props.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, SummarizationResponse.class.getPackageName());
        try (Consumer<String, SummarizationResponse> consumer =
                     new DefaultKafkaConsumerFactory<String, SummarizationResponse>(props).createConsumer()) {
            consumer.subscribe(List.of(responsesTopic));
            ConsumerRecords<String, SummarizationResponse> records =
                    KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(15));
            assertThat(records).isNotEmpty();
            return records.iterator().next();
        }
    }

    private static SummarizationRequest request() {
        Instant now = Instant.parse("2026-11-12T10:00:00Z");
        return new SummarizationRequest(
                UUID.randomUUID(), UUID.randomUUID(),
                now.minus(Duration.ofDays(1)), now,
                List.of(
                        new SummarizationTask("Сделано", "DONE", now, now.minus(Duration.ofHours(2))),
                        new SummarizationTask("Осталось", "TODO", null, now.minus(Duration.ofHours(1)))));
    }
}
