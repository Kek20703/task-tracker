package com.tasktracker.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void apiDescriptionIsPublicAndCoversAllEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/user'].post").exists())
                .andExpect(jsonPath("$.paths['/user'].get").exists())
                .andExpect(jsonPath("$.paths['/auth/login'].post").exists())
                .andExpect(jsonPath("$.paths['/tasks'].get").exists())
                .andExpect(jsonPath("$.paths['/tasks'].post").exists())
                .andExpect(jsonPath("$.paths['/tasks/{id}'].patch").exists())
                .andExpect(jsonPath("$.paths['/tasks/{id}'].delete").exists())
                .andExpect(jsonPath("$.paths['/error']").doesNotExist())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"));
    }

    @Test
    void registrationAndLoginDoNotRequireToken() throws Exception {
        // Пустой список требований — так в OpenAPI помечаются открытые методы.
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths['/user'].post.security").value(empty()))
                .andExpect(jsonPath("$.paths['/auth/login'].post.security").value(empty()))
                .andExpect(jsonPath("$.paths['/tasks'].get.security").doesNotExist())
                .andExpect(jsonPath("$.security[0].bearerAuth").exists());
    }

    @Test
    void registrationAndLoginTakeJsonBodyWithoutQueryParameters() throws Exception {
        // Метод для формы скрыт, иначе springdoc сливает его с JSON-методом, и пароль
        // попадает в описание как обязательный параметр строки запроса.
        for (String path : new String[] {"/user", "/auth/login"}) {
            String operation = "$.paths['" + path + "'].post";
            mockMvc.perform(get("/v3/api-docs"))
                    .andExpect(jsonPath(operation + ".parameters").doesNotExist())
                    .andExpect(jsonPath(operation + ".operationId").value(endsWith("Json")))
                    .andExpect(jsonPath(operation + ".requestBody.content['application/json']").exists())
                    .andExpect(jsonPath(operation + ".responses['200'].headers.Authorization").exists())
                    .andExpect(jsonPath(operation + ".responses['200'].content['application/json'].schema['$ref']")
                            .value(endsWith("/UserResponse")));
        }
    }

    @Test
    void taskResponsesHaveActualStatusCodes() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths['/tasks'].post.responses['201'].headers.Location").exists())
                .andExpect(jsonPath("$.paths['/tasks'].post.responses['201'].content['application/json'].schema['$ref']")
                        .value(endsWith("/TaskResponse")))
                .andExpect(jsonPath("$.paths['/tasks'].post.responses['200']").doesNotExist())
                .andExpect(jsonPath("$.paths['/tasks/{id}'].delete.responses['204']").exists())
                .andExpect(jsonPath("$.paths['/tasks/{id}'].delete.responses['200']").doesNotExist());
    }

    @Test
    void currentUserParameterIsNotExposedAsRequestParameter() throws Exception {
        // @AuthenticationPrincipal берётся из токена, а не из запроса — в описании его быть не должно.
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths['/user'].get.parameters").doesNotExist())
                .andExpect(jsonPath("$.paths['/tasks'].get.parameters").doesNotExist())
                .andExpect(jsonPath("$.paths['/tasks/{id}'].delete.parameters[*].name").value(contains("id")));
    }

    @Test
    void swaggerUiIsPublic() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("swagger-ui")));
        // Короткий адрес из README перенаправляет на страницу выше.
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void onlyGetIsOpenOnDocumentationPaths() throws Exception {
        mockMvc.perform(post("/v3/api-docs"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required"));
        mockMvc.perform(post("/swagger-ui/index.html"))
                .andExpect(status().isUnauthorized());
    }
}
