package com.example.taskmanager.mapper;

import com.example.taskmanager.dto.user.UserResponse;
import com.example.taskmanager.entity.User;

/**
 * Converts a {@link User} entity into a safe API representation.
 * The password hash is never part of the result.
 */
public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getCreatedAt()
        );
    }
}
