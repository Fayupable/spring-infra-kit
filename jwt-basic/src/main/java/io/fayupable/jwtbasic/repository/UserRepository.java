package io.fayupable.jwtbasic.repository;

import io.fayupable.jwtbasic.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * User Repository
 * <p>
 * Handles database operations for User entity.
 * <p>
 * Key methods:
 * - findByEmail: Used during login
 * - existsByEmail: Used during registration to check duplicates
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find user by email
     * <p>
     * This is the PRIMARY method for authentication.
     * <p>
     * Used in:
     * - Login process (to verify credentials)
     * - JWT token validation (to load user details)
     *
     * @param email User's email address
     * @return Optional containing user if found, empty otherwise
     * <p>
     * Why Optional?
     * - Avoids null pointer exceptions
     * - Forces us to handle "user not found" case explicitly
     * <p>
     * Spring Data JPA generates:
     * SELECT * FROM users WHERE email = ?
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if user exists by email
     * <p>
     * Used during registration to prevent duplicate accounts.
     *
     * @param email Email to check
     * @return true if email is already registered, false otherwise
     * <p>
     * Why use this instead of findByEmail?
     * - More efficient: returns boolean instead of entire entity
     * - Clearer intent: we only care if it exists, not the data
     * <p>
     * Spring Data JPA generates:
     * SELECT COUNT(*) > 0 FROM users WHERE email = ?
     */
    boolean existsByEmail(String email);
}