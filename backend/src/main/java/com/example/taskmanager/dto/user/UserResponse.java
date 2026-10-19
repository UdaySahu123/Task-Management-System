package com.example.taskmanager.dto.user;

import java.time.LocalDateTime;

/**
 * Public representation of a user returned by the API.
 *
 * <p>Deliberately excludes {@code passwordHash} and any other sensitive
 * data so it can never leak through a response.</p>
 */
public record UserResponse(
        Long id,
        String name,
        String email,
        LocalDateTime createdAt
) {
}
