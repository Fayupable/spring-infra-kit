package io.fayupable.jwtrefreshtoken.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Registration Request DTO
 * <p>
 * Used when new user registers.
 * Contains user details and credentials.
 * <p>
 * Validation:
 * - Email must be valid and unique
 * - Password must be at least 6 characters
 * - Username is optional
 */
@Data
public class RegisterRequest {

    /**
     * User's email address
     * Must be unique in the system
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    /**
     * User's password (plain text)
     * Will be hashed with BCrypt before storing
     */
    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    /**
     * Username (optional)
     * If not provided, email will be used
     */
    @Size(max = 30, message = "Username must be less than 30 characters")
    private String username;
}