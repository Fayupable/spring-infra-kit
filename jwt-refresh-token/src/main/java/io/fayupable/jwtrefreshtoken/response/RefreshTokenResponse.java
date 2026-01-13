package io.fayupable.jwtrefreshtoken.response;

import lombok.Builder;

/**
 * Refresh Token Response DTO
 * <p>
 * Returned after successful token refresh.
 * Contains new access token and new refresh token.
 * <p>
 * Token Rotation:
 * - Old refresh token is revoked
 * - New refresh token is created
 * - New access token is generated
 */
@Builder
public record RefreshTokenResponse(
        String accessToken,
        String refreshToken,
        String type,
        Long expiresIn,
        boolean success,
        String message
) {
    /**
     * Canonical constructor with default value for type
     */
    public RefreshTokenResponse {
        type = (type != null) ? type : "Bearer";
    }
}