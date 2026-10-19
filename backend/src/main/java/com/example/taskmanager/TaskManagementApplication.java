package com.example.taskmanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the Task Management System backend.
 *
 * <p>Run locally with: {@code mvnw spring-boot:run} (or from an IDE).
 * The application reads its configuration from environment variables,
 * which can be placed in a {@code .env} file next to this project.</p>
 */
@SpringBootApplication
public class TaskManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskManagementApplication.class, args);
    }
}
