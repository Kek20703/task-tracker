package com.tasktracker.backend.dto;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class EmailMessageTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void jsonMatchesConsumerContract() {
        String json = jsonMapper.writeValueAsString(
                new EmailMessage("user@example.com", "Добро пожаловать", "Текст письма"));

        assertThat(json).isEqualTo(
                "{\"to\":\"user@example.com\",\"subject\":\"Добро пожаловать\",\"text\":\"Текст письма\"}");
    }
}
