package com.example.taskmanager.exception;

/**
 * Thrown when a user tries to register (or change their email to) an address
 * that already exists. Mapped to HTTP 409 Conflict.
 */
public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String email) {
        super("An account with email '" + email + "' already exists");
    }
}
