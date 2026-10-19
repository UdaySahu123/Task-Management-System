package com.example.taskmanager.service;

import com.example.taskmanager.dto.common.PageResponse;
import com.example.taskmanager.dto.task.TaskRequest;
import com.example.taskmanager.dto.task.TaskResponse;
import com.example.taskmanager.entity.Task;
import com.example.taskmanager.entity.TaskPriority;
import com.example.taskmanager.entity.TaskStatus;
import com.example.taskmanager.entity.User;
import com.example.taskmanager.exception.BadRequestException;
import com.example.taskmanager.exception.ResourceNotFoundException;
import com.example.taskmanager.mapper.TaskMapper;
import com.example.taskmanager.repository.TaskRepository;
import com.example.taskmanager.repository.UserRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * All task business logic.
 *
 * <p><strong>Ownership rule:</strong> every method receives the authenticated
 * user id and scopes its queries by it. A task that exists but belongs to
 * somebody else is reported as "not found" (404), so attackers cannot even
 * discover that a task id exists.</p>
 */
@Service
public class TaskService {

    /** Whitelist of sortable fields: anything else is rejected with a 400. */
    private static final Set<String> SORTABLE_FIELDS =
            Set.of("createdAt", "updatedAt", "dueDate", "priority", "title", "status");

    private static final int MAX_PAGE_SIZE = 100;

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public TaskService(TaskRepository taskRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    /** Creates a task owned by the authenticated user. */
    @Transactional
    public TaskResponse create(Long userId, TaskRequest request) {
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + userId));

        Task task = new Task(
                request.title().trim(),
                normalizeDescription(request.description()),
                request.status() != null ? request.status() : TaskStatus.TODO,
                request.priority() != null ? request.priority() : TaskPriority.MEDIUM,
                request.dueDate(),
                owner);

        return TaskMapper.toResponse(taskRepository.save(task));
    }

    /** Loads one task, but only if the authenticated user owns it. */
    @Transactional(readOnly = true)
    public TaskResponse getById(Long userId, Long taskId) {
        return TaskMapper.toResponse(findOwnedTask(userId, taskId));
    }

    /**
     * Paginated, filtered and sorted list of the user's tasks.
     *
     * @param search   optional case-insensitive title search
     * @param status   optional status filter
     * @param priority optional priority filter
     * @param overdue  when true, only overdue (not completed, past due) tasks
     * @param sort     "field,direction" e.g. {@code dueDate,asc}
     */
    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> list(Long userId,
                                           String search,
                                           TaskStatus status,
                                           TaskPriority priority,
                                           Boolean overdue,
                                           String sort,
                                           int page,
                                           int size) {

        if (page < 0) {
            throw new BadRequestException("Page index must not be negative");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("Page size must be between 1 and " + MAX_PAGE_SIZE);
        }

        SortSpec sortSpec = parseSort(sort);
        Pageable pageable = PageRequest.of(page, size);
        Specification<Task> spec = buildSpecification(userId, search, status, priority, overdue, sortSpec);

        Page<Task> result = taskRepository.findAll(spec, pageable);
        return PageResponse.from(result, TaskMapper::toResponse);
    }

    /** Full update of a task. Status/priority default to the current values when omitted. */
    @Transactional
    public TaskResponse update(Long userId, Long taskId, TaskRequest request) {
        Task task = findOwnedTask(userId, taskId);

        task.setTitle(request.title().trim());
        task.setDescription(normalizeDescription(request.description()));
        task.setDueDate(request.dueDate());
        if (request.status() != null) {
            task.setStatus(request.status());
        }
        if (request.priority() != null) {
            task.setPriority(request.priority());
        }

        return TaskMapper.toResponse(taskRepository.save(task));
    }

    /** Quick status change (used by the dropdown on the task cards). */
    @Transactional
    public TaskResponse updateStatus(Long userId, Long taskId, TaskStatus status) {
        Task task = findOwnedTask(userId, taskId);
        task.setStatus(status);
        return TaskMapper.toResponse(taskRepository.save(task));
    }

    /** Quick priority change. */
    @Transactional
    public TaskResponse updatePriority(Long userId, Long taskId, TaskPriority priority) {
        Task task = findOwnedTask(userId, taskId);
        task.setPriority(priority);
        return TaskMapper.toResponse(taskRepository.save(task));
    }

    /** Deletes a task owned by the user. Throws 404 when it does not exist / is not theirs. */
    @Transactional
    public void delete(Long userId, Long taskId) {
        long removed = taskRepository.deleteByIdAndUserId(taskId, userId);
        if (removed == 0) {
            throw new ResourceNotFoundException("Task not found with id " + taskId);
        }
    }

    /**
     * Central ownership check. Never trust a client-provided user id:
     * the {@code userId} always comes from the JWT principal.
     */
    private Task findOwnedTask(Long userId, Long taskId) {
        return taskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id " + taskId));
    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        return description.trim();
    }

    /** Parses "dueDate,asc" into a validated {@link SortSpec}. */
    private SortSpec parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return new SortSpec("createdAt", false); // newest first by default
        }
        String[] parts = sort.split(",");
        String field = parts[0].trim();
        if (!SORTABLE_FIELDS.contains(field)) {
            throw new BadRequestException("Unsupported sort field '" + field
                    + "'. Allowed: " + String.join(", ", SORTABLE_FIELDS));
        }
        boolean ascending = parts.length > 1 && parts[1].trim().equalsIgnoreCase("asc");
        return new SortSpec(field, ascending);
    }

    /** Builds a dynamic WHERE clause plus ORDER BY, always scoped to the owner. */
    private Specification<Task> buildSpecification(Long userId, String search, TaskStatus status,
                                                   TaskPriority priority, Boolean overdue, SortSpec sort) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Ownership: the single most important predicate.
            predicates.add(cb.equal(root.get("user").get("id"), userId));

            if (search != null && !search.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("title")), "%" + search.trim().toLowerCase() + "%"));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }
            if (Boolean.TRUE.equals(overdue)) {
                predicates.add(cb.and(
                        cb.notEqual(root.get("status"), TaskStatus.COMPLETED),
                        cb.lessThan(root.get("dueDate"), LocalDate.now())));
            }

            // Apply ORDER BY only for the data query, never the count query.
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                applyOrdering(query, cb, root, sort);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private void applyOrdering(CriteriaQuery<?> query, CriteriaBuilder cb, Root<Task> root, SortSpec sort) {
        Expression<?> key;

        if ("priority".equals(sort.field())) {
            // Sort by logical priority (HIGH, MEDIUM, LOW) instead of alphabetically.
            key = cb.<Integer>selectCase()
                    .when(cb.equal(root.get("priority"), TaskPriority.HIGH), 1)
                    .when(cb.equal(root.get("priority"), TaskPriority.MEDIUM), 2)
                    .otherwise(3);
        } else {
            key = root.get(sort.field());
        }

        query.orderBy(sort.ascending() ? cb.asc(key) : cb.desc(key));
    }

    /** Validated sort request. */
    private record SortSpec(String field, boolean ascending) {
    }
}
