package com.example.taskmanager.exception;

/**
 * Thrown when a request is syntactically valid but semantically wrong,
 * for example when changing a password with an incorrect current password.
 * Mapped to HTTP 400 Bad Request.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
