package com.tasktracker.backend.exception;

import com.tasktracker.backend.dto.ErrorDto;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;

/** Юнит-тесты общего обработчика: коды ответов и тексты ошибок. */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    // --- Свои исключения ---

    @Test
    void emailAlreadyTakenIsConflict() {
        ResponseEntity<ErrorDto> response = handler.handleEmailAlreadyTaken(new EmailAlreadyTakenException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isEqualTo(new ErrorDto("This email is already taken"));
    }

    @Test
    void missingTaskIsNotFound() {
        ResponseEntity<ErrorDto> response = handler.handleTaskNotFound(new TaskNotFoundException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo(new ErrorDto("Task not found"));
    }

    @Test
    void invalidCredentialsAreUnauthorized() {
        ResponseEntity<ErrorDto> response = handler.handleInvalidCredentials(new InvalidCredentialsException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isEqualTo(new ErrorDto("Invalid email or password"));
    }

    // --- Исключения безопасности ---

    @Test
    void authenticationFailureInsideServiceIsServerError() {
        // Например, недоступна база при проверке пароля: это не вина клиента.
        ResponseEntity<ErrorDto> response =
                handler.handleAuthenticationFailure(new InternalAuthenticationServiceException("db down"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo(new ErrorDto("Internal server error"));
    }

    @Test
    void otherAuthenticationErrorsAreUnauthorized() {
        ResponseEntity<ErrorDto> response = handler.handleAuthentication(new BadCredentialsException("bad"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isEqualTo(new ErrorDto("Authentication is required"));
    }

    @Test
    void accessDeniedIsForbidden() {
        ResponseEntity<ErrorDto> response = handler.handleAccessDenied(new AccessDeniedException("denied"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isEqualTo(new ErrorDto("Access is denied"));
    }

    @Test
    void unexpectedErrorIsServerErrorWithoutDetails() {
        ResponseEntity<ErrorDto> response = handler.handleUnexpected(new IllegalStateException("secret details"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo(new ErrorDto("Internal server error"));
    }

    // --- Стандартные исключения Spring MVC: тело подменяется на своё ---

    @Test
    void validationErrorsAreJoinedSortedAndDeduplicated() throws Exception {
        MethodArgumentNotValidException ex = validationError("Title is required", "Email is invalid", "Title is required");

        ResponseEntity<Object> response = handleInternal(ex, HttpStatus.BAD_REQUEST);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(new ErrorDto("Email is invalid; Title is required"));
    }

    @Test
    void validationErrorWithoutMessagesHasGenericText() throws Exception {
        ResponseEntity<Object> response = handleInternal(validationError(""), HttpStatus.BAD_REQUEST);

        assertThat(response.getBody()).isEqualTo(new ErrorDto("Request is not valid"));
    }

    @Test
    void malformedBodyHasOwnText() {
        HttpMessageNotReadableException ex =
                new HttpMessageNotReadableException("JSON parse error", (HttpInputMessage) null);

        ResponseEntity<Object> response = handleInternal(ex, HttpStatus.BAD_REQUEST);

        assertThat(response.getBody()).isEqualTo(new ErrorDto("Request body is missing or malformed"));
    }

    @Test
    void standardStatusUsesReasonPhrase() {
        ResponseEntity<Object> response = handleInternal(new IllegalStateException("x"), HttpStatus.METHOD_NOT_ALLOWED);

        assertThat(response.getBody()).isEqualTo(new ErrorDto("Method Not Allowed"));
    }

    @Test
    void unknownStatusCodeHasGenericText() {
        ResponseEntity<Object> response = handleInternal(new IllegalStateException("x"), HttpStatusCode.valueOf(499));

        assertThat(response.getBody()).isEqualTo(new ErrorDto("Request failed"));
    }

    @Test
    void responseHeadersArePreserved() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.ALLOW, "GET");

        ResponseEntity<Object> response =
                handler.handleExceptionInternal(new IllegalStateException("x"), null, headers,
                        HttpStatus.METHOD_NOT_ALLOWED, null);

        assertThat(response.getHeaders().getFirst(HttpHeaders.ALLOW)).isEqualTo("GET");
    }

    private ResponseEntity<Object> handleInternal(Exception ex, HttpStatusCode status) {
        return handler.handleExceptionInternal(ex, null, new HttpHeaders(), status, null);
    }

    private static MethodArgumentNotValidException validationError(String... messages) throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        for (int i = 0; i < messages.length; i++) {
            bindingResult.addError(new FieldError("request", "field" + i, messages[i]));
        }
        MethodParameter parameter = new MethodParameter(String.class.getDeclaredMethod("length"), -1);
        return new MethodArgumentNotValidException(parameter, bindingResult);
    }
}
