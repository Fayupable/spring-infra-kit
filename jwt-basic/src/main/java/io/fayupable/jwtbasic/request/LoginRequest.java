package io.fayupable.jwtbasic.request;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Login Request DTO
 * <p>
 * Used when user tries to login.
 * Contains email and password.
 */
@Data
public class LoginRequest {

    /**
     * User's email address
     * Must be a valid email format
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    /**
     * User's password (plain text)
     * Will be compared with hashed password in database
     */
    @NotBlank(message = "Password is required")
    private String password;
}