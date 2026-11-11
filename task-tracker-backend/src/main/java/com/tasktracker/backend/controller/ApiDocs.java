package com.tasktracker.backend.controller;

import com.tasktracker.backend.dto.UserResponse;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Общие фрагменты описания API для Swagger UI. */
final class ApiDocs {

    static final String FORM_ALSO_ACCEPTED =
            "Тело можно передать и как application/x-www-form-urlencoded с теми же полями. "
                    + "Токен выдаётся в заголовке ответа Authorization: Bearer <token>.";

    private ApiDocs() {
    }

    /** Ответ на регистрацию и вход: пользователь в теле, токен в заголовке. */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponse(
            responseCode = "200",
            description = "Пользователь; access token — в заголовке Authorization",
            headers = @Header(
                    name = "Authorization",
                    description = "Bearer <token> — токен для кнопки Authorize (без слова Bearer)",
                    schema = @Schema(type = "string")),
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class)))
    @interface TokenInResponseHeader {
    }
}
