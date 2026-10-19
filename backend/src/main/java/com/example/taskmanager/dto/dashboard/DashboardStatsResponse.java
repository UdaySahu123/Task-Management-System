package com.example.taskmanager.dto.dashboard;

import com.example.taskmanager.dto.task.TaskResponse;

import java.util.List;

/**
 * Everything the dashboard needs, calculated from real database rows for the
 * currently authenticated user (nothing is hard-coded).
 *
 * @param totalTasks           all tasks owned by the user
 * @param pendingTasks         tasks with status TODO
 * @param inProgressTasks      tasks with status IN_PROGRESS
 * @param completedTasks       tasks with status COMPLETED
 * @param overdueTasks         not completed and past their due date
 * @param completionPercentage completedTasks / totalTasks * 100, rounded
 * @param recentTasks          five most recently created tasks
 * @param upcomingDeadlines    five next deadlines (due today or later, not completed)
 */
public record DashboardStatsResponse(
        long totalTasks,
        long pendingTasks,
        long inProgressTasks,
        long completedTasks,
        long overdueTasks,
        int completionPercentage,
        List<TaskResponse> recentTasks,
        List<TaskResponse> upcomingDeadlines
) {
}
