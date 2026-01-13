package io.fayupable.jwtrefreshtoken.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Refresh Token Request DTO
 *
 * Used when client wants to refresh access token.
 * Contains the refresh token received during login.
 *
 * Flow:
 * 1. Client's access token expires (15 min)
 * 2. Client sends refresh token to get new access token
 * 3. Server validates refresh token
 * 4. Server returns new access token + new refresh token
 */
@Data
public class RefreshTokenRequest {

    /**
     * Refresh token string
     *
     * This is the plain token (not hashed).
     * Server will hash it and compare with database.
     */
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}