package com.tasktracker.scheduler.service;

import com.tasktracker.scheduler.dto.EmailMessage;
import org.apache.kafka.common.KafkaException;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailServiceTest {

    private final KafkaTemplate<Object, Object> kafkaTemplate = mock(KafkaTemplate.class);
    private final EmailService service = new EmailService(kafkaTemplate, "EMAIL_SENDING_TASKS");
    private final EmailMessage message = new EmailMessage("user@example.com", "Тема", "Текст");

    @Test
    void sendsEmailToTopic() {
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.<SendResult<Object, Object>>completedFuture(null));

        service.send(message);

        verify(kafkaTemplate).send(eq("EMAIL_SENDING_TASKS"), eq("user@example.com"), eq(message));
    }

    @Test
    void synchronousKafkaFailureDoesNotEscape() {
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenThrow(new KafkaException("down"));

        assertThatCode(() -> service.send(message)).doesNotThrowAnyException();
    }
}
