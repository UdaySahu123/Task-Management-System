package com.example.taskmanager.dto.task;

import com.example.taskmanager.entity.TaskStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Payload for {@code PATCH /api/tasks/{id}/status}.
 */
public record TaskStatusUpdateRequest(

        @NotNull(message = "Status is required")
        TaskStatus status
) {
}
