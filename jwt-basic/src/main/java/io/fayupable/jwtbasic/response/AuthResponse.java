package io.fayupable.jwtbasic.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Authentication Response DTO
 *
 * Returned after successful login or registration.
 * Contains JWT token and basic user info.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    /**
     * JWT access token
     * Client must include this in Authorization header for protected endpoints
     * Format: "Bearer <token>"
     */
    private String token;

    /**
     * Token type (always "Bearer")
     */
    @Builder.Default
    private String type = "Bearer";

    /**
     * User's email
     */
    private String email;

    /**
     * User's roles
     * Example: ["ROLE_USER"] or ["ROLE_USER", "ROLE_ADMIN"]
     */
    private List<String> roles;
}