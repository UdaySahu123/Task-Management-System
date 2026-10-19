package com.example.taskmanager.dto.task;

import com.example.taskmanager.entity.TaskPriority;
import com.example.taskmanager.entity.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Payload used to create ({@code POST}) or fully update ({@code PUT}) a task.
 *
 * <p>Status and priority are optional; the service applies sensible defaults
 * ({@code TODO} / {@code MEDIUM}) when they are omitted.</p>
 */
public record TaskRequest(

        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must be at most 200 characters")
        String title,

        @Size(max = 2000, message = "Description must be at most 2000 characters")
        String description,

        TaskStatus status,

        TaskPriority priority,

        @NotNull(message = "Due date is required")
        LocalDate dueDate
) {
}
