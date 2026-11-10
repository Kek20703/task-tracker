package com.tasktracker.backend.exception;

import com.tasktracker.backend.dto.ErrorDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.stream.Collectors;

/**
 * Приводит все ошибки REST API к виду {"message": "..."}.
 * Стандартные исключения Spring MVC (404, 405, 415, невалидное тело и т.п.)
 * обрабатывает базовый класс, здесь только подменяется тело ответа.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(EmailAlreadyTakenException.class)
    public ResponseEntity<ErrorDto> handleEmailAlreadyTaken(EmailAlreadyTakenException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorDto(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDto> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorDto("Internal server error"));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        return ResponseEntity.status(statusCode).headers(headers).body(new ErrorDto(messageFor(ex, statusCode)));
    }

    private static String messageFor(Exception ex, HttpStatusCode statusCode) {
        if (ex instanceof MethodArgumentNotValidException notValid) {
            return notValid.getBindingResult().getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .sorted()
                    .distinct()
                    .collect(Collectors.joining("; "));
        }
        if (ex instanceof HttpMessageNotReadableException) {
            return "Request body is missing or malformed";
        }
        if (ex instanceof org.springframework.web.ErrorResponse errorResponse
                && errorResponse.getBody().getDetail() != null) {
            return errorResponse.getBody().getDetail();
        }
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        return status != null ? status.getReasonPhrase() : "Request failed";
    }
}
