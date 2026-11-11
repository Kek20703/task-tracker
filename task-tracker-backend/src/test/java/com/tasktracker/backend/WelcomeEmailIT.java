package com.tasktracker.backend;

import com.tasktracker.backend.dto.EmailMessage;
import com.tasktracker.backend.repository.UserRepository;
import com.tasktracker.backend.service.WelcomeEmailService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.kafka.KafkaContainer;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import({TestcontainersConfiguration.class, KafkaTestcontainersConfiguration.class})
@SpringBootTest
@AutoConfigureMockMvc
class WelcomeEmailIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private KafkaContainer kafkaContainer;

    @BeforeEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    @Test
    void registrationPublishesWelcomeEmailWithoutTypeHeaders() throws Exception {
        mockMvc.perform(post("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"User@Example.com\", \"password\": \"secret-password\"}"))
                .andExpect(status().isOk());

        ConsumerRecord<String, EmailMessage> record = readMessage();

        assertThat(record.key()).isEqualTo("user@example.com");
        assertThat(record.headers().lastHeader("__TypeId__")).isNull();
        assertThat(record.value()).isEqualTo(new EmailMessage(
                "user@example.com",
                "Добро пожаловать в Task Tracker",
                "Здравствуйте! Вы успешно зарегистрировались в Task Tracker. "
                        + "Теперь можно создавать задачи и отмечать выполненные."));
    }

    private ConsumerRecord<String, EmailMessage> readMessage() {
        Map<String, Object> props = KafkaTestUtils.consumerProps(
                kafkaContainer.getBootstrapServers(), "welcome-email-check", true);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class);
        props.put(JacksonJsonDeserializer.VALUE_DEFAULT_TYPE, EmailMessage.class.getName());
        props.put(JacksonJsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        props.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, EmailMessage.class.getPackageName());

        try (Consumer<String, EmailMessage> consumer =
                     new DefaultKafkaConsumerFactory<String, EmailMessage>(props).createConsumer()) {
            consumer.subscribe(List.of(WelcomeEmailService.EMAIL_SENDING_TASKS));
            ConsumerRecords<String, EmailMessage> records =
                    KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(15));
            assertThat(records).hasSize(1);
            return records.iterator().next();
        }
    }
}
