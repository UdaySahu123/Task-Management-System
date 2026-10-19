package com.example.taskmanager.controller;

import com.example.taskmanager.dto.user.ChangePasswordRequest;
import com.example.taskmanager.dto.user.UpdateProfileRequest;
import com.example.taskmanager.dto.user.UserResponse;
import com.example.taskmanager.security.SecurityUtils;
import com.example.taskmanager.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Profile endpoints for the authenticated user.
 *
 * <pre>
 * GET /api/users/me           -> current profile
 * PUT /api/users/me           -> update name / email
 * PUT /api/users/me/password  -> change password
 * </pre>
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getProfile() {
        return ResponseEntity.ok(userService.getProfile(SecurityUtils.currentUserId()));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(SecurityUtils.currentUserId(), request));
    }

    @PutMapping("/me/password")
    public ResponseEntity<Map<String, String>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(SecurityUtils.currentUserId(), request);
        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }
}
