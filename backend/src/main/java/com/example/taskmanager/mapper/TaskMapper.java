package com.example.taskmanager.mapper;

import com.example.taskmanager.dto.task.TaskResponse;
import com.example.taskmanager.entity.Task;

/**
 * Converts between the {@link Task} entity and its API representation.
 * Keeping this logic in one place avoids leaking entity details into the API.
 */
public final class TaskMapper {

    private TaskMapper() {
    }

    public static TaskResponse toResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getDueDate(),
                task.isOverdue(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
