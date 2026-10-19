package com.example.taskmanager.exception;

/**
 * Thrown when a requested resource does not exist <em>or</em> does not belong
 * to the current user. Both cases intentionally return HTTP 404 so that an
 * attacker cannot probe which task IDs exist on other accounts.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException task(Long id) {
        return new ResourceNotFoundException("Task not found with id: " + id);
    }

    public static ResourceNotFoundException user(Long id) {
        return new ResourceNotFoundException("User not found with id: " + id);
    }
}
