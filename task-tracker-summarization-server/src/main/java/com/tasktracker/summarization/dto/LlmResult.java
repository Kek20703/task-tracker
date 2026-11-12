package com.tasktracker.summarization.dto;

/** Результат LLM или шаблона: текст отчёта и признак, что ответил LLM. */
public record LlmResult(String report, boolean usedLlm) {
}
