package com.example.taskmanager.security;

import com.example.taskmanager.entity.User;
import com.example.taskmanager.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bridges the database with Spring Security.
 *
 * <ul>
 *   <li>{@link #loadUserByUsername(String)} is used during login (username = email).</li>
 *   <li>{@link #loadUserById(Long)} is used by the JWT filter to rebuild the
 *       principal from the token's subject claim.</li>
 * </ul>
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("No user found with email " + email));
        return UserPrincipal.from(user);
    }

    /** Used by JwtAuthenticationFilter. A deleted user produces a UsernameNotFoundException. */
    @Transactional(readOnly = true)
    public UserPrincipal loadUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User " + id + " no longer exists"));
        return UserPrincipal.from(user);
    }
}
