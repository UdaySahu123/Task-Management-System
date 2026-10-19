package com.example.taskmanager.service;

import com.example.taskmanager.dto.dashboard.DashboardStatsResponse;
import com.example.taskmanager.dto.task.TaskResponse;
import com.example.taskmanager.entity.Task;
import com.example.taskmanager.entity.TaskStatus;
import com.example.taskmanager.mapper.TaskMapper;
import com.example.taskmanager.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Builds the dashboard summary for the authenticated user.
 *
 * <p>Every number comes straight from the database (scoped by user id) —
 * nothing is hard-coded or cached, so the dashboard is always consistent
 * with the task list.</p>
 */
@Service
public class DashboardService {

    private final TaskRepository taskRepository;

    public DashboardService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Transactional(readOnly = true)
    public DashboardStatsResponse getStats(Long userId) {
        long total = taskRepository.countByUserId(userId);
        long pending = taskRepository.countByUserIdAndStatus(userId, TaskStatus.TODO);
        long inProgress = taskRepository.countByUserIdAndStatus(userId, TaskStatus.IN_PROGRESS);
        long completed = taskRepository.countByUserIdAndStatus(userId, TaskStatus.COMPLETED);
        long overdue = taskRepository.countOverdueTasks(userId, TaskStatus.COMPLETED, LocalDate.now());

        int completionPercentage = total == 0 ? 0 : (int) Math.round(completed * 100.0 / total);

        List<TaskResponse> recentTasks = taskRepository.findTop5ByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(TaskMapper::toResponse)
                .toList();

        List<Task> upcoming = taskRepository
                .findTop5ByUserIdAndStatusNotAndDueDateGreaterThanEqualOrderByDueDateAsc(
                        userId, TaskStatus.COMPLETED, LocalDate.now());

        List<TaskResponse> upcomingDeadlines = upcoming.stream()
                .map(TaskMapper::toResponse)
                .toList();

        return new DashboardStatsResponse(
                total, pending, inProgress, completed, overdue,
                completionPercentage, recentTasks, upcomingDeadlines);
    }
}
