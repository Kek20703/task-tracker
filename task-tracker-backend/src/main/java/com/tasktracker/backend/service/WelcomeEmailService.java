package com.tasktracker.backend.service;

import com.tasktracker.backend.dto.EmailMessage;
import com.tasktracker.backend.dto.UserResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Отправляет приветственное письмо после успешной регистрации.
 *
 * <p>Публикация не должна ломать регистрацию: ошибки Kafka логируются, но наружу не выходят.
 * Если регистрация выполняется в транзакции, сообщение уходит только после её фиксации.
 */
@Service
public class WelcomeEmailService {

    public static final String EMAIL_SENDING_TASKS = "EMAIL_SENDING_TASKS";
    public static final String SUBJECT = "Добро пожаловать в Task Tracker";
    public static final String TEXT = "Здравствуйте! Вы успешно зарегистрировались в Task Tracker. "
            + "Теперь можно создавать задачи и отмечать выполненные.";

    private static final Logger log = LoggerFactory.getLogger(WelcomeEmailService.class);

    private final KafkaTemplate<Object, Object> kafkaTemplate;

    public WelcomeEmailService(KafkaTemplate<Object, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void send(UserResponse user) {
        EmailMessage message = new EmailMessage(user.email(), SUBJECT, TEXT);
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publish(user, message);
                }
            });
        } else {
            publish(user, message);
        }
    }

    private void publish(UserResponse user, EmailMessage message) {
        try {
            kafkaTemplate.send(EMAIL_SENDING_TASKS, user.email(), message)
                    .whenComplete((result, error) -> {
                        if (error != null) {
                            log.warn("Не удалось отправить приветственное письмо пользователю {}",
                                    user.id(), error);
                        }
                    });
        } catch (RuntimeException e) {
            log.warn("Не удалось отправить приветственное письмо пользователю {}", user.id(), e);
        }
    }
}
