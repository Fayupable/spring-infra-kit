package io.fayupable.jwtrefreshtoken.exception;

import org.springframework.security.core.AuthenticationException;

/**
 * Token Expired Exception
 * <p>
 * Thrown when JWT access token or refresh token has expired.
 * <p>
 * Scenarios:
 * - Access token expired (15 min passed)
 * - Refresh token sliding window expired (30 days inactive)
 * - Refresh token absolute expiry reached (90 days max)
 * <p>
 * HTTP Response:
 * - Status: 401 Unauthorized
 * - Message: Specific expiry reason
 * <p>
 * Client Action:
 * - Access token expired → Try refresh endpoint
 * - Refresh token expired → Redirect to login page
 * <p>
 * Security Note:
 * Extends AuthenticationException so Spring Security handles it properly.
 */
public class TokenExpiredException extends AuthenticationException {

    /**
     * Constructor with message
     *
     * @param message Specific expiry reason (e.g., "Access token expired")
     */
    public TokenExpiredException(String message) {
        super(message);
    }

    /**
     * Constructor with message and cause
     *
     * @param message Specific expiry reason
     * @param cause   Root cause exception (e.g., ExpiredJwtException)
     */
    public TokenExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
}