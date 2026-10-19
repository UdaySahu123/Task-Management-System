package com.example.taskmanager.dto.auth;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Payload for {@code POST /api/auth/register}.
 *
 * <p>Every field is validated on the server even though the React form
 * validates them too: client-side validation is a convenience, never a
 * security control.</p>
 */
public record RegisterRequest(

        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Please provide a valid email address")
        @Size(max = 150, message = "Email must be at most 150 characters")
        String email,

        /*
         * Password policy:
         *  - between 8 and 72 characters (BCrypt ignores anything past 72 bytes)
         *  - at least one uppercase letter, one lowercase letter and one digit
         */
        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
                message = "Password must contain at least one uppercase letter, one lowercase letter and one digit")
        String password,

        @NotBlank(message = "Please confirm your password")
        String confirmPassword
) {

    /**
     * Cross-field validation: the two passwords must match.
     * Bean Validation automatically invokes any {@code isXxx()} method
     * annotated with {@code @AssertTrue}.
     */
    @AssertTrue(message = "Passwords do not match")
    public boolean isPasswordConfirmed() {
        return password != null && password.equals(confirmPassword);
    }
}
