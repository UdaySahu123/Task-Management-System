package com.example.taskmanager.service;

import com.example.taskmanager.dto.user.ChangePasswordRequest;
import com.example.taskmanager.dto.user.UpdateProfileRequest;
import com.example.taskmanager.dto.user.UserResponse;
import com.example.taskmanager.entity.User;
import com.example.taskmanager.exception.BadRequestException;
import com.example.taskmanager.exception.DuplicateEmailException;
import com.example.taskmanager.exception.ResourceNotFoundException;
import com.example.taskmanager.mapper.UserMapper;
import com.example.taskmanager.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Profile operations for the authenticated user.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(Long userId) {
        return UserMapper.toResponse(requireUser(userId));
    }

    /** Updates name and email. Email stays unique across all accounts. */
    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = requireUser(userId);
        String newEmail = request.email().trim().toLowerCase();

        if (!user.getEmail().equalsIgnoreCase(newEmail)
                && userRepository.existsByEmailIgnoreCase(newEmail)) {
            throw new DuplicateEmailException(newEmail);
        }

        user.setName(request.name().trim());
        user.setEmail(newEmail);

        return UserMapper.toResponse(userRepository.save(user));
    }

    /**
     * Changes the password after verifying the current one.
     * The new password is hashed with BCrypt before storage.
     */
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + userId));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new BadRequestException("New password must be different from the current password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + userId));
    }
}
