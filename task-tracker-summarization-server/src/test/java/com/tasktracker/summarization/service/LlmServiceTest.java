package com.tasktracker.summarization.service;

import com.tasktracker.summarization.dto.LlmResult;
import com.tasktracker.summarization.dto.SummarizationRequest;
import com.tasktracker.summarization.dto.SummarizationTask;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class LlmServiceTest {

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

    @Test
    void returnsLlmTextWhenConfigured() {
        server.expect(requestTo("http://llm/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"choices\":[{\"message\":{\"content\":\"Отчёт от LLM\"}}]}",
                        MediaType.APPLICATION_JSON));

        LlmResult result = service("http://llm", "key").summarize(request());

        assertThat(result.usedLlm()).isTrue();
        assertThat(result.report()).isEqualTo("Отчёт от LLM");
        server.verify();
    }

    @Test
    void returnsTemplateWhenKeyIsMissing() {
        LlmResult result = service("http://llm", "").summarize(request());

        assertThat(result.usedLlm()).isFalse();
        assertThat(result.report()).contains("Всего задач: 2, выполнено: 1, осталось: 1");
    }

    @Test
    void returnsTemplateWhenLlmFails() {
        server.expect(requestTo("http://llm/chat/completions"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        LlmResult result = service("http://llm", "key").summarize(request());

        assertThat(result.usedLlm()).isFalse();
        assertThat(result.report()).contains("Всего задач: 2");
        server.verify();
    }

    private LlmService service(String baseUrl, String apiKey) {
        return new LlmService(builder.build(), baseUrl, apiKey, "test-model");
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
