package com.example.taskmanager.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Small helper to read the authenticated user from the SecurityContext.
 *
 * <p>Controllers use {@link #currentUserId()} to pass ownership into the
 * service layer. The value comes exclusively from the validated JWT, so a
 * client can never impersonate another user by changing a request body.</p>
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static UserPrincipal currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            // Should never happen for an endpoint protected by Spring Security.
            throw new IllegalStateException("No authenticated user in the security context");
        }
        return principal;
    }

    public static Long currentUserId() {
        return currentUser().getId();
    }
}
