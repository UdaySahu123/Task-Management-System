package com.example.taskmanager.dto.auth;

import com.example.taskmanager.dto.user.UserResponse;

/**
 * Successful authentication response.
 *
 * <p>The JWT is returned in the response body (not a cookie) and the React
 * client sends it back as {@code Authorization: Bearer <token>} on every
 * request. The token never contains the password hash.</p>
 *
 * @param token     signed JWT
 * @param tokenType always "Bearer"
 * @param expiresIn token lifetime in milliseconds
 * @param user      public profile of the authenticated user
 */
public record AuthResponse(
        String token,
        String tokenType,
        long expiresIn,
        UserResponse user
) {
    public static AuthResponse bearer(String token, long expiresIn, UserResponse user) {
        return new AuthResponse(token, "Bearer", expiresIn, user);
    }
}
