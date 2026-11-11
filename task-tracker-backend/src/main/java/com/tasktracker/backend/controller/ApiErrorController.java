package com.tasktracker.backend.controller;

import com.tasktracker.backend.dto.ErrorDto;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ошибки, возникшие вне контроллеров (их контейнер перенаправляет на /error),
 * отдаются в том же формате {"message": "..."}, что и остальные.
 */
@Hidden
@RestController
public class ApiErrorController implements ErrorController {

    @RequestMapping("${server.error.path:/error}")
    public ResponseEntity<ErrorDto> error(HttpServletRequest request) {
        HttpStatus status = resolveStatus(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE));
        return ResponseEntity.status(status).body(new ErrorDto(status.getReasonPhrase()));
    }

    private static HttpStatus resolveStatus(Object statusCode) {
        if (statusCode instanceof Integer code) {
            HttpStatus status = HttpStatus.resolve(code);
            if (status != null) {
                return status;
            }
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
