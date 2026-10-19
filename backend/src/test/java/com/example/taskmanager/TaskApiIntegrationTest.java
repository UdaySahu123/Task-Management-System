package com.example.taskmanager;

import com.example.taskmanager.dto.auth.AuthResponse;
import com.example.taskmanager.dto.auth.RegisterRequest;
import com.example.taskmanager.dto.task.TaskRequest;
import com.example.taskmanager.dto.task.TaskResponse;
import com.example.taskmanager.entity.TaskPriority;
import com.example.taskmanager.entity.TaskStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end API tests running against an in-memory H2 database.
 *
 * <p>These tests exercise the whole stack — security filters, JWT validation,
 * validation, persistence, ownership rules — through real HTTP requests.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaskApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ---------------------------------------------------------------- helpers

    private AuthResponse registerUser(String label) throws Exception {
        String email = label + "." + UUID.randomUUID() + "@example.com";
        RegisterRequest request = new RegisterRequest("User " + label, email, "Passw0rd123", "Passw0rd123");
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);
    }

    private TaskResponse createTask(String token, TaskRequest request) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), TaskResponse.class);
    }

    private static TaskRequest task(String title, TaskStatus status, TaskPriority priority, LocalDate due) {
        return new TaskRequest(title, "description", status, priority, due);
    }

    // ---------------------------------------------------------------- auth

    @Test
    @DisplayName("register + login + /me round trip")
    void authRoundTrip() throws Exception {
        AuthResponse auth = registerUser("auth");
        assertThat(auth.token()).isNotBlank();
        assertThat(auth.user().email()).contains("@example.com");

        // The response must never contain a password hash field.
        MvcResult me = mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + auth.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(auth.user().email()))
                .andReturn();

        assertThat(me.getResponse().getContentAsString()).doesNotContain("passwordHash");
    }

    @Test
    @DisplayName("registering an existing email returns 409")
    void duplicateEmailReturns409() throws Exception {
        AuthResponse first = registerUser("dup");
        RegisterRequest again = new RegisterRequest("Someone Else", first.user().email(),
                "Passw0rd123", "Passw0rd123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(again)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("requests without or with an invalid token return 401")
    void unauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/tasks")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/tasks").header("Authorization", "Bearer garbage"))
                .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------- validation

    @Test
    @DisplayName("invalid task payloads return 400 with field errors")
    void invalidPayloadReturns400() throws Exception {
        AuthResponse auth = registerUser("val");

        // blank title
        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + auth.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"   \",\"dueDate\":\"" + LocalDate.now() + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("title"));

        // missing due date
        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + auth.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"No due date\"}"))
                .andExpect(status().isBadRequest());

        // invalid enum value
        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + auth.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Bad\",\"status\":\"BLOCKED\",\"dueDate\":\"" + LocalDate.now() + "\"}"))
                .andExpect(status().isBadRequest());

        // malformed JSON
        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + auth.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ not valid json "))
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------- ownership

    @Test
    @DisplayName("a user can never read, modify or delete another user's task")
    void ownershipIsolation() throws Exception {
        AuthResponse alice = registerUser("alice");
        AuthResponse bob = registerUser("bob");

        TaskResponse aliceTask = createTask(alice.token(),
                task("Alice private task", TaskStatus.TODO, TaskPriority.HIGH, LocalDate.now().plusDays(2)));

        // Bob cannot read it...
        mockMvc.perform(get("/api/tasks/" + aliceTask.id())
                        .header("Authorization", "Bearer " + bob.token()))
                .andExpect(status().isNotFound());

        // ...nor update it...
        mockMvc.perform(put("/api/tasks/" + aliceTask.id())
                        .header("Authorization", "Bearer " + bob.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                task("Hijacked", TaskStatus.TODO, TaskPriority.LOW, LocalDate.now()))))
                .andExpect(status().isNotFound());

        // ...nor delete it...
        mockMvc.perform(delete("/api/tasks/" + aliceTask.id())
                        .header("Authorization", "Bearer " + bob.token()))
                .andExpect(status().isNotFound());

        // ...and Bob's task list is still empty.
        mockMvc.perform(get("/api/tasks").header("Authorization", "Bearer " + bob.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        // Alice still sees her task.
        mockMvc.perform(get("/api/tasks").header("Authorization", "Bearer " + alice.token()))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // ---------------------------------------------------------------- list

    @Test
    @DisplayName("search, status/priority filters, sorting and pagination")
    void searchFilterSortPagination() throws Exception {
        AuthResponse auth = registerUser("list");
        String token = auth.token();

        createTask(token, task("Write report", TaskStatus.TODO, TaskPriority.HIGH, LocalDate.now().plusDays(3)));
        createTask(token, task("Review report", TaskStatus.IN_PROGRESS, TaskPriority.MEDIUM, LocalDate.now().plusDays(5)));
        createTask(token, task("Deploy app", TaskStatus.COMPLETED, TaskPriority.LOW, LocalDate.now().plusDays(1)));
        createTask(token, task("Fix bug", TaskStatus.TODO, TaskPriority.HIGH, LocalDate.now().minusDays(1)));

        // search
        mockMvc.perform(get("/api/tasks?search=report").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.totalElements").value(2));

        // status filter
        mockMvc.perform(get("/api/tasks?status=TODO").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.totalElements").value(2));

        // priority filter
        mockMvc.perform(get("/api/tasks?priority=HIGH").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.totalElements").value(2));

        // overdue filter (due yesterday + not completed)
        mockMvc.perform(get("/api/tasks?overdue=true").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Fix bug"));

        // sort by due date ascending
        mockMvc.perform(get("/api/tasks?sort=dueDate,asc").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.content[0].title").value("Fix bug"));

        // pagination
        mockMvc.perform(get("/api/tasks?page=0&size=2").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.totalElements").value(4));

        // unsupported sort field
        mockMvc.perform(get("/api/tasks?sort=passwordHash,asc").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------- dashboard

    @Test
    @DisplayName("dashboard statistics are calculated from real rows for the current user")
    void dashboardStatistics() throws Exception {
        AuthResponse auth = registerUser("dash");
        String token = auth.token();

        createTask(token, task("Todo one", TaskStatus.TODO, TaskPriority.HIGH, LocalDate.now().plusDays(1)));
        createTask(token, task("Todo two", TaskStatus.TODO, TaskPriority.LOW, LocalDate.now().plusDays(2)));
        createTask(token, task("Doing", TaskStatus.IN_PROGRESS, TaskPriority.MEDIUM, LocalDate.now().plusDays(3)));
        createTask(token, task("Done", TaskStatus.COMPLETED, TaskPriority.MEDIUM, LocalDate.now().minusDays(5)));
        createTask(token, task("Late", TaskStatus.TODO, TaskPriority.HIGH, LocalDate.now().minusDays(2)));

        mockMvc.perform(get("/api/dashboard/stats").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTasks").value(5))
                .andExpect(jsonPath("$.pendingTasks").value(3))
                .andExpect(jsonPath("$.inProgressTasks").value(1))
                .andExpect(jsonPath("$.completedTasks").value(1))
                .andExpect(jsonPath("$.overdueTasks").value(1))
                .andExpect(jsonPath("$.completionPercentage").value(20))
                .andExpect(jsonPath("$.recentTasks.length()").value(5))
                .andExpect(jsonPath("$.upcomingDeadlines").isArray());
    }

    // ---------------------------------------------------------------- delete

    @Test
    @DisplayName("deleting a task removes it; fetching it afterwards returns 404")
    void deleteTask() throws Exception {
        AuthResponse auth = registerUser("del");
        String token = auth.token();
        TaskResponse created = createTask(token,
                task("Temp task", TaskStatus.TODO, TaskPriority.LOW, LocalDate.now().plusDays(1)));

        mockMvc.perform(delete("/api/tasks/" + created.id())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/tasks/" + created.id())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("changing status and priority through PATCH works")
    void patchStatusAndPriority() throws Exception {
        AuthResponse auth = registerUser("patch");
        String token = auth.token();
        TaskResponse created = createTask(token,
                task("Patch me", TaskStatus.TODO, TaskPriority.LOW, LocalDate.now().plusDays(1)));

        mockMvc.perform(patch("/api/tasks/" + created.id() + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(patch("/api/tasks/" + created.id() + "/priority")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"priority\":\"HIGH\"}"))
                .andExpect(jsonPath("$.priority").value("HIGH"));
    }
}
