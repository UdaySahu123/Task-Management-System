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
import com.example.taskmanager.repository.TaskRepository;
import com.example.taskmanager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    private static final Long OWNER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private UserRepository userRepository;

    private TaskService taskService;

    private User owner;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(taskRepository, userRepository);
        owner = user(OWNER_ID, "Alice", "alice@example.com");
    }

    // ---------------------------------------------------------------- helpers

    private static User user(Long id, String name, String email) {
        User user = new User(name, email, "$2a$hash");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private static Task task(Long id, String title, TaskStatus status, TaskPriority priority,
                             LocalDate dueDate, User owner) {
        Task task = new Task(title, "description", status, priority, dueDate, owner);
        ReflectionTestUtils.setField(task, "id", id);
        return task;
    }

    private static TaskRequest request(String title, TaskStatus status, TaskPriority priority, LocalDate due) {
        return new TaskRequest(title, "description", status, priority, due);
    }

    // ---------------------------------------------------------------- create

    @Test
    @DisplayName("create: saves the task with the authenticated user as owner")
    void create_setsOwner() {
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            ReflectionTestUtils.setField(t, "id", 10L);
            return t;
        });

        TaskResponse response = taskService.create(OWNER_ID,
                request("New task", TaskStatus.TODO, TaskPriority.HIGH, LocalDate.now().plusDays(1)));

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.title()).isEqualTo("New task");
        assertThat(response.status()).isEqualTo(TaskStatus.TODO);
        assertThat(response.priority()).isEqualTo(TaskPriority.HIGH);

        verify(taskRepository).save(any(Task.class));
    }

    @Test
    @DisplayName("create: applies TODO/MEDIUM defaults when status and priority are omitted")
    void create_appliesDefaults() {
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskResponse response = taskService.create(OWNER_ID,
                new TaskRequest("No status", null, null, null, LocalDate.now().plusDays(2)));

        assertThat(response.status()).isEqualTo(TaskStatus.TODO);
        assertThat(response.priority()).isEqualTo(TaskPriority.MEDIUM);
    }

    // ---------------------------------------------------------------- read

    @Test
    @DisplayName("getById: returns the task when it belongs to the user")
    void getById_owned() {
        Task t = task(5L, "Mine", TaskStatus.TODO, TaskPriority.LOW, LocalDate.now(), owner);
        when(taskRepository.findByIdAndUserId(5L, OWNER_ID)).thenReturn(Optional.of(t));

        TaskResponse response = taskService.getById(OWNER_ID, 5L);

        assertThat(response.title()).isEqualTo("Mine");
    }

    @Test
    @DisplayName("getById: another user's task is reported as not found (404, no existence leak)")
    void getById_notOwner_throwsNotFound() {
        // The ownership-scoped query returns empty for a foreign task.
        when(taskRepository.findByIdAndUserId(5L, OTHER_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getById(OTHER_USER_ID, 5L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Task not found");
    }

    // ---------------------------------------------------------------- update

    @Test
    @DisplayName("update: changes all editable fields of an owned task")
    void update_changesFields() {
        Task t = task(5L, "Old", TaskStatus.TODO, TaskPriority.LOW, LocalDate.now(), owner);
        when(taskRepository.findByIdAndUserId(5L, OWNER_ID)).thenReturn(Optional.of(t));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        LocalDate newDue = LocalDate.now().plusDays(10);
        TaskResponse response = taskService.update(OWNER_ID, 5L,
                request("New title", TaskStatus.IN_PROGRESS, TaskPriority.HIGH, newDue));

        assertThat(response.title()).isEqualTo("New title");
        assertThat(response.status()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(response.priority()).isEqualTo(TaskPriority.HIGH);
        assertThat(response.dueDate()).isEqualTo(newDue);
    }

    @Test
    @DisplayName("update: cannot update a task owned by somebody else")
    void update_notOwner_throwsNotFound() {
        when(taskRepository.findByIdAndUserId(5L, OTHER_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.update(OTHER_USER_ID, 5L,
                request("Hijack", TaskStatus.TODO, TaskPriority.HIGH, LocalDate.now())))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    @DisplayName("updateStatus: changes only the status")
    void updateStatus_works() {
        Task t = task(5L, "Task", TaskStatus.TODO, TaskPriority.LOW, LocalDate.now(), owner);
        when(taskRepository.findByIdAndUserId(5L, OWNER_ID)).thenReturn(Optional.of(t));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskResponse response = taskService.updateStatus(OWNER_ID, 5L, TaskStatus.COMPLETED);

        assertThat(response.status()).isEqualTo(TaskStatus.COMPLETED);
    }

    @Test
    @DisplayName("updatePriority: changes only the priority")
    void updatePriority_works() {
        Task t = task(5L, "Task", TaskStatus.TODO, TaskPriority.LOW, LocalDate.now(), owner);
        when(taskRepository.findByIdAndUserId(5L, OWNER_ID)).thenReturn(Optional.of(t));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskResponse response = taskService.updatePriority(OWNER_ID, 5L, TaskPriority.HIGH);

        assertThat(response.priority()).isEqualTo(TaskPriority.HIGH);
    }

    // ---------------------------------------------------------------- delete

    @Test
    @DisplayName("delete: removes an owned task")
    void delete_owned() {
        when(taskRepository.deleteByIdAndUserId(5L, OWNER_ID)).thenReturn(1L);

        taskService.delete(OWNER_ID, 5L);

        verify(taskRepository).deleteByIdAndUserId(5L, OWNER_ID);
    }

    @Test
    @DisplayName("delete: missing or foreign task throws not found")
    void delete_notFound_throws() {
        when(taskRepository.deleteByIdAndUserId(5L, OWNER_ID)).thenReturn(0L);

        assertThatThrownBy(() -> taskService.delete(OWNER_ID, 5L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------------------------------------------------------------- list

    @Test
    @DisplayName("list: maps a repository page into a PageResponse")
    void list_returnsPagedResults() {
        Task t1 = task(1L, "First", TaskStatus.TODO, TaskPriority.HIGH, LocalDate.now(), owner);
        Task t2 = task(2L, "Second", TaskStatus.COMPLETED, TaskPriority.LOW, LocalDate.now(), owner);
        Page<Task> page = new PageImpl<>(List.of(t1, t2));

        when(taskRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResponse<TaskResponse> response =
                taskService.list(OWNER_ID, "search term", TaskStatus.TODO, TaskPriority.HIGH, false,
                        "createdAt,desc", 0, 10);

        assertThat(response.content()).hasSize(2);
        assertThat(response.totalElements()).isEqualTo(2);
        assertThat(response.content().get(0).title()).isEqualTo("First");
    }

    @Test
    @DisplayName("list: rejects an unsupported sort field")
    void list_rejectsUnknownSortField() {
        assertThatThrownBy(() -> taskService.list(OWNER_ID, null, null, null, null,
                "passwordHash,asc", 0, 10))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("list: rejects an out-of-range page size")
    void list_rejectsInvalidPageSize() {
        assertThatThrownBy(() -> taskService.list(OWNER_ID, null, null, null, false, null, 0, 5000))
                .isInstanceOf(BadRequestException.class);
    }
}
