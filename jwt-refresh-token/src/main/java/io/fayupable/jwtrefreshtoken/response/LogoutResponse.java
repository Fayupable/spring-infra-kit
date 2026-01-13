package io.fayupable.jwtrefreshtoken.response;

import lombok.Builder;

/**
 * Logout Response DTO
 * <p>
 * Returned after successful logout.
 * Indicates that refresh token was revoked.
 */
@Builder
public record LogoutResponse(
        boolean success,
        String message,
        boolean shouldClearTokens
        ) {
}