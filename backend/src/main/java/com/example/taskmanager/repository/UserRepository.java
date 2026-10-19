package com.example.taskmanager.repository;

import com.example.taskmanager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Data access for {@link User}. Spring Data JPA generates the queries
 * from the method names, so no implementation is needed.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /** Case-insensitive lookup so "UDAY@mail.com" and "uday@mail.com" are the same account. */
    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
