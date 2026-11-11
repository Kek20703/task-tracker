package com.tasktracker.backend.service;

import com.tasktracker.backend.dto.EmailMessage;
import com.tasktracker.backend.dto.UserResponse;
import org.apache.kafka.common.KafkaException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class WelcomeEmailServiceTest {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<Object, Object> kafkaTemplate = mock(KafkaTemplate.class);
    private final WelcomeEmailService service = new WelcomeEmailService(kafkaTemplate);
    private final UserResponse user = new UserResponse(42L, "user@example.com");

    @Test
    void sendsWelcomeEmailWithEmailAsKey() {
        CompletableFuture<SendResult<Object, Object>> sent = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(sent);

        service.send(user);

        ArgumentCaptor<EmailMessage> message = ArgumentCaptor.forClass(EmailMessage.class);
        verify(kafkaTemplate).send(eq(WelcomeEmailService.EMAIL_SENDING_TASKS),
                eq("user@example.com"), message.capture());
        assertThat(message.getValue()).isEqualTo(new EmailMessage(
                "user@example.com",
                "Добро пожаловать в Task Tracker",
                "Здравствуйте! Вы успешно зарегистрировались в Task Tracker. "
                        + "Теперь можно создавать задачи и отмечать выполненные."));
    }

    @Test
    void sendsOnlyAfterCommitWhenTransactionIsActive() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            service.send(user);
            verifyNoInteractions(kafkaTemplate);

            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit);

            verify(kafkaTemplate).send(eq(WelcomeEmailService.EMAIL_SENDING_TASKS),
                    eq("user@example.com"), any(EmailMessage.class));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }

    @Test
    void synchronousKafkaFailureDoesNotBreakRegistration() {
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenThrow(new KafkaException("down"));

        assertThatCode(() -> service.send(user)).doesNotThrowAnyException();
    }

    @Test
    void asynchronousKafkaFailureDoesNotBreakRegistration() {
        CompletableFuture<SendResult<Object, Object>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new KafkaException("down"));
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(failed);

        assertThatCode(() -> service.send(user)).doesNotThrowAnyException();
    }
}
