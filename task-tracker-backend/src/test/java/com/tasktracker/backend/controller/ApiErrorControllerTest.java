package com.tasktracker.backend.controller;

import com.tasktracker.backend.dto.ErrorDto;
import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ApiErrorControllerTest {

    private final ApiErrorController controller = new ApiErrorController();

    @Test
    void usesStatusCodeFromRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 404);

        ResponseEntity<ErrorDto> response = controller.error(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo(new ErrorDto("Not Found"));
    }

    @Test
    void unknownStatusCodeBecomesInternalServerError() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 499);

        assertThat(controller.error(request).getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void missingOrNonNumericStatusCodeBecomesInternalServerError() {
        assertThat(controller.error(new MockHttpServletRequest()).getStatusCode())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, "404");
        assertThat(controller.error(request).getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
