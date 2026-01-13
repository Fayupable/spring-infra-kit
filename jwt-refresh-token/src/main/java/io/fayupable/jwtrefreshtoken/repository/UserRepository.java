package io.fayupable.jwtrefreshtoken.repository;

import io.fayupable.jwtrefreshtoken.entity.User;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * User Repository
 * <p>
 * Data access layer for User entity.
 * Provides methods to query users from database.
 * <p>
 * Spring Data JPA automatically implements these methods.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find user by email
     * <p>
     * Primary method for user authentication.
     * Email is used as username in this system.
     * <p>
     * Used during:
     * - Login (find user by email)
     * - Token validation (load user details)
     * - Password reset
     *
     * @param email User's email address
     * @return Optional containing user if found, empty otherwise
     * <p>
     * Example:
     * Optional<User> user = userRepository.findByEmail("user@example.com");
     * if (user.isPresent()) {
     * // Authenticate
     * }
     * <p>
     * Query generated: SELECT * FROM users WHERE email = ?
     */
    Optional<User> findByEmail(String email);

    /**
     * Find user by Username OR Email
     * <p>
     * Primary method for "Smart Login".
     * Allows users to log in using either their username or their email address.
     * <p>
     * Logic:
     * Checks if the provided string matches the 'username' column
     * OR matches the 'email' column.
     * <p>
     * Used during:
     * - Login (AuthenticationManager calls this via UserDetailsService)
     *
     * @param username Value to check against username column
     * @param email    Value to check against email column (usually same input)
     * @return Optional containing user if found in either column
     * <p>
     * Example:
     * // User enters "fayupable" (username) -> Found
     * // User enters "test@mail.com" (email) -> Found
     * userRepository.findByUsernameOrEmail(input, input);
     * <p>
     * Query generated: SELECT * FROM users WHERE username = ? OR email = ?
     */
    Optional<User> findByUsernameOrEmail(String username, String email);

    /**
     * Check if email already exists
     * <p>
     * Used during registration to prevent duplicate accounts.
     * More efficient than findByEmail when you only need existence check.
     *
     * @param email Email to check
     * @return true if email exists, false otherwise
     * <p>
     * Example:
     * if (userRepository.existsByEmail("user@example.com")) {
     * throw new RuntimeException("Email already in use");
     * }
     * <p>
     * Query generated: SELECT COUNT(*) > 0 FROM users WHERE email = ?
     */
    boolean existsByEmail(String email);

    /**
     * Check if username already exists
     * <p>
     * Used during registration to prevent duplicate usernames.
     * More efficient than findByUsername when you only need existence check.
     *
     * @param username Username to check
     * @return true if username exists, false otherwise
     * <p>
     * Example:
     * if (userRepository.existsByUsername("fayupable")) {
     * throw new RuntimeException("Username already taken");
     * }
     * <p>
     * Query generated: SELECT COUNT(*) > 0 FROM users WHERE username = ?
     */
    boolean existsByUsername(@Size(max = 30, message = "Username must be less than 30 characters") String username);
}