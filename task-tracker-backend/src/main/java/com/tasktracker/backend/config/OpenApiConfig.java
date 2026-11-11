package com.tasktracker.backend.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Описание API для Swagger UI ({@code /swagger-ui.html}).
 * Все методы, кроме регистрации и входа, требуют JWT: токен приходит в заголовке {@code Authorization}
 * ответа на {@code POST /user} или {@code POST /auth/login}, его нужно вставить в окно Authorize
 * без префикса {@code Bearer}.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Task Tracker API",
                version = "1",
                description = "Многопользовательский планировщик задач. Ошибки всегда приходят как {\"message\": \"...\"}."),
        security = @SecurityRequirement(name = OpenApiConfig.BEARER))
@SecurityScheme(
        name = OpenApiConfig.BEARER,
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Access token из заголовка Authorization ответа на регистрацию или вход — без слова Bearer.")
public class OpenApiConfig {

    static final String BEARER = "bearerAuth";
}
