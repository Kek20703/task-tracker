package com.tasktracker.summarization.service;

import com.tasktracker.summarization.dto.LlmResult;
import com.tasktracker.summarization.dto.SummarizationRequest;
import com.tasktracker.summarization.dto.SummarizationTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Клиент OpenAI-совместимого API. Без ключа и URL работает заглушка; ошибка или таймаут
 * не пробрасываются наружу, а дают шаблонный отчёт.
 */
@Service
public class LlmService {

    private static final Logger log = LoggerFactory.getLogger(LlmService.class);
    private static final String SYSTEM_PROMPT = """
            Ты составляешь краткий отчёт по задачам пользователя.
            Пиши по-русски, только факты из списка задач.
            Формат: 2-4 предложения — что сделано и что осталось.
            Не выдумывай задачи и не добавляй советов.
            """;

    private final RestClient client;
    private final String chatUrl;
    private final String apiKey;
    private final String model;
    private final boolean configured;

    public LlmService(RestClient llmRestClient,
                      @Value("${llm.base-url:}") String baseUrl,
                      @Value("${llm.api-key:}") String apiKey,
                      @Value("${llm.model:gpt-4o-mini}") String model) {
        this.client = llmRestClient;
        this.apiKey = apiKey;
        this.model = model;
        this.configured = !baseUrl.isBlank() && !apiKey.isBlank();
        this.chatUrl = configured ? baseUrl.replaceAll("/+$", "") + "/chat/completions" : null;
    }

    public LlmResult summarize(SummarizationRequest request) {
        String fallback = fallback(request);
        if (!configured) {
            return new LlmResult(fallback, false);
        }
        try {
            RestClient.RequestBodySpec spec = client.post()
                    .uri(chatUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
            ChatResponse response = spec
                    .body(new ChatRequest(model, List.of(
                            new Message("system", SYSTEM_PROMPT),
                            new Message("user", prompt(request)))))
                    .retrieve()
                    .body(ChatResponse.class);
            String text = response == null ? null : response.firstText();
            if (text == null || text.isBlank()) {
                return new LlmResult(fallback, false);
            }
            return new LlmResult(text.strip(), true);
        } catch (RuntimeException e) {
            log.warn("LLM request failed, using template report", e);
            return new LlmResult(fallback, false);
        }
    }

    private static String prompt(SummarizationRequest request) {
        StringBuilder prompt = new StringBuilder()
                .append("Период: ").append(request.from()).append(" — ").append(request.to()).append('\n')
                .append("Задачи:\n");
        List<SummarizationTask> tasks = request.tasks() == null ? List.of() : request.tasks();
        if (tasks.isEmpty()) {
            prompt.append("нет задач\n");
        } else {
            for (SummarizationTask task : tasks) {
                prompt.append("- ").append(task.status()).append(": ").append(task.title())
                        .append(" (создана ").append(task.createdAt());
                if (task.completedAt() != null) {
                    prompt.append(", выполнена ").append(task.completedAt());
                }
                prompt.append(")\n");
            }
        }
        return prompt.toString();
    }

    static String fallback(SummarizationRequest request) {
        List<SummarizationTask> tasks = request.tasks() == null ? List.of() : request.tasks();
        long done = tasks.stream().filter(task -> "DONE".equals(task.status())).count();
        long left = tasks.size() - done;
        return "Отчёт за период с " + request.from() + " по " + request.to() + ". "
                + "Всего задач: " + tasks.size() + ", выполнено: " + done + ", осталось: " + left + ".";
    }

    public record ChatRequest(String model, List<Message> messages) {
    }

    public record Message(String role, String content) {
    }

    public record ChatResponse(List<Choice> choices) {

        String firstText() {
            if (choices == null || choices.isEmpty() || choices.get(0).message() == null) {
                return null;
            }
            return choices.get(0).message().content();
        }
    }

    public record Choice(Message message) {
    }
}
