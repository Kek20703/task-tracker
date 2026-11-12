package com.tasktracker.scheduler.service;

import com.tasktracker.scheduler.dto.EmailMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final String topic;

    public EmailService(KafkaTemplate<Object, Object> kafkaTemplate,
                        @Value("${email.topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void send(EmailMessage message) {
        try {
            kafkaTemplate.send(topic, message.to(), message)
                    .whenComplete((result, error) -> {
                        if (error != null) {
                            log.warn("Failed to publish daily digest email for {}", message.to(), error);
                        }
                    });
        } catch (RuntimeException e) {
            log.warn("Failed to publish daily digest email for {}", message.to(), e);
        }
    }
}
