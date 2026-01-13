package io.fayupable.jwtrefreshtoken.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Login Request DTO
 * <p>
 * Used when user attempts to login.
 * Contains credentials for authentication.
 * <p>
 * Validation:
 * - Email must be valid format and not blank
 * - Password must not be blank
 */
@Data
public class LoginRequest {

    /**
     * Accepts either Email or Username
     * Can contain "user@example.com" OR "myusername"
     */
    @NotBlank(message = "Username or Email is required")
    private String usernameOrEmail;

    /**
     * User's password (plain text)
     * Will be compared with BCrypt hash in database
     */
    @NotBlank(message = "Password is required")
    private String password;
}