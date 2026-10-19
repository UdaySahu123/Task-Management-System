package com.example.taskmanager.dto.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Consistent JSON error body returned for every failed request.
 *
 * <pre>
 * {
 *   "timestamp": "2026-01-01T10:15:30",
 *   "status": 400,
 *   "error": "Bad Request",
 *   "message": "Validation failed",
 *   "path": "/api/tasks",
 *   "fieldErrors": [ { "field": "title", "message": "Title is required" } ]
 * }
 * </pre>
 *
 * <p>Stack traces, SQL details and internal exception messages are never
 * exposed here.</p>
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonPropertyOrder({"timestamp", "status", "error", "message", "path", "fieldErrors"})
public record ApiError(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldError> fieldErrors
) {

    public record FieldError(String field, String message) {
    }

    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(LocalDateTime.now(), status, error, message, path, List.of());
    }

    public static ApiError of(int status, String error, String message, String path,
                              List<FieldError> fieldErrors) {
        return new ApiError(LocalDateTime.now(), status, error, message, path, fieldErrors);
    }
}
