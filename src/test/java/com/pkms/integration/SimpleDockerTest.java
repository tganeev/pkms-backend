package com.pkms.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@SpringBootTest
public class SimpleDockerTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Test
    void testPostgresContainer() throws Exception {
        // Проверяем, что контейнер запущен
        assertTrue(postgres.isRunning());

        // Выводим информацию о контейнере
        System.out.println("=== DOCKER CONTAINER INFO ===");
        System.out.println("Container ID: " + postgres.getContainerId());
        System.out.println("Container is running: " + postgres.isRunning());
        System.out.println("JDBC URL: " + postgres.getJdbcUrl());
        System.out.println("Database: " + postgres.getDatabaseName());
        System.out.println("Username: " + postgres.getUsername());
        System.out.println("==============================");

        Thread.sleep(10000000);
    }
}