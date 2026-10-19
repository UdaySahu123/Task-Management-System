package com.example.taskmanager.repository;

import com.example.taskmanager.entity.Task;
import com.example.taskmanager.entity.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Data access for {@link Task}.
 *
 * <p>Every ownership sensitive lookup filters by both task id and user id, so
 * an attacker can never reach another user's task by guessing a task ID.</p>
 *
 * <p>{@code JpaSpecificationExecutor} adds type-safe dynamic filtering
 * (search / status / priority / overdue) for the task list endpoint.</p>
 */
public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    /** Loads a single task only if it belongs to the given user. */
    Optional<Task> findByIdAndUserId(Long id, Long userId);

    /** Deletes a task only if it belongs to the given user. Returns the number of rows removed. */
    long deleteByIdAndUserId(Long id, Long userId);

    /** Total number of tasks owned by a user (dashboard). */
    long countByUserId(Long userId);

    /** Number of tasks owned by a user with a given status (dashboard). */
    long countByUserIdAndStatus(Long userId, TaskStatus status);

    /**
     * Number of overdue tasks: not completed and due before {@code today}.
     * The completed status is passed as a parameter to keep the query portable
     * across databases (H2 in tests, MySQL in production).
     */
    @Query("""
            SELECT COUNT(t) FROM Task t
            WHERE t.user.id = :userId
              AND t.status <> :completedStatus
              AND t.dueDate < :today
            """)
    long countOverdueTasks(@Param("userId") Long userId,
                           @Param("completedStatus") TaskStatus completedStatus,
                           @Param("today") LocalDate today);

    /** Five most recently created tasks, for the dashboard "recent tasks" panel. */
    List<Task> findTop5ByUserIdOrderByCreatedAtDesc(Long userId);

    /** Upcoming deadlines: not completed, due today or later, soonest first. */
    List<Task> findTop5ByUserIdAndStatusNotAndDueDateGreaterThanEqualOrderByDueDateAsc(
            Long userId, TaskStatus status, LocalDate from);
}
