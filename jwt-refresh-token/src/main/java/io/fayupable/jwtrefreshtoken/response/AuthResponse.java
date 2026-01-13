package io.fayupable.jwtrefreshtoken.response;

import lombok.Builder;

import java.util.List;

/**
 * Authentication Response DTO
 * <p>
 * Returned after successful login or registration.
 * Contains both access token and refresh token.
 */
@Builder
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String type,
        String email,
        List<String> roles,
        Long expiresIn
) {
    /**
     * Canonical constructor with default value for type
     */
    public AuthResponse {
        type = (type != null) ? type : "Bearer";
    }
}