package com.tasktracker.backend;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    /** Секрет только для тестов; настоящий задаётся переменной окружения JWT_SECRET. */
    static final String TEST_JWT_SECRET = "test-only-jwt-secret-0123456789-abcdefghij";

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"));
    }

    @Bean
    DynamicPropertyRegistrar testSecrets() {
        return registry -> registry.add("jwt.secret", () -> TEST_JWT_SECRET);
    }
}
