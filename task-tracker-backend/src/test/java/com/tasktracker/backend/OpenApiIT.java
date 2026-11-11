package com.tasktracker.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    void currentUserParameterIsNotExposedAsRequestParameter() throws Exception {
        // @AuthenticationPrincipal берётся из токена, а не из запроса — в описании его быть не должно.
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths['/user'].get.parameters").doesNotExist())
                .andExpect(jsonPath("$.paths['/tasks'].get.parameters").doesNotExist())
                .andExpect(jsonPath("$.paths['/tasks/{id}'].delete.parameters[*].name").value(org.hamcrest.Matchers.contains("id")));
    }

    @Test
    void swaggerUiIsPublic() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("swagger-ui")));
    }
}
