package com.example.taskmanager.dto.task;

import com.example.taskmanager.entity.TaskPriority;
import jakarta.validation.constraints.NotNull;

/**
 * Payload for {@code PATCH /api/tasks/{id}/priority}.
 */
public record TaskPriorityUpdateRequest(

        @NotNull(message = "Priority is required")
        TaskPriority priority
) {
}
