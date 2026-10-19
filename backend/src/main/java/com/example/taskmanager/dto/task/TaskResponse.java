package com.example.taskmanager.dto.task;

import com.example.taskmanager.entity.TaskPriority;
import com.example.taskmanager.entity.TaskStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Task representation returned by the API.
 *
 * <p>{@code overdue} is computed on the server so every client shows the
 * same value without duplicating business rules.</p>
 */
public record TaskResponse(
        Long id,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        LocalDate dueDate,
        boolean overdue,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
