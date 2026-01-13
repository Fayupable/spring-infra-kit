package io.fayupable.jwtrefreshtoken.exception;

import org.springframework.security.core.AuthenticationException;

/**
 * Token Blacklisted Exception
 * <p>
 * Thrown when access token is found in blacklist.
 * <p>
 * Scenarios:
 * - User logged out but token not yet expired
 * - Admin revoked user's session
 * - Security breach detected, token blacklisted
 * - Password changed, all tokens invalidated
 * <p>
 * HTTP Response:
 * - Status: 401 Unauthorized
 * - Message: "Token has been revoked. Please login again."
 * <p>
 * Client Action:
 * - Clear all tokens
 * - Redirect to login page
 * - Show "Session expired" message
 * <p>
 * Blacklist Storage:
 * - Redis (if available)
 * - In-Memory (fallback)
 * - TTL: Remaining token lifetime
 * <p>
 * Security Note:
 * This prevents token reuse after logout/revocation.
 * Critical for logout security.
 */
public class TokenBlacklistedException extends AuthenticationException {

    /**
     * Constructor with message
     *
     * @param message Blacklist reason (e.g., "Token revoked after logout")
     */
    public TokenBlacklistedException(String message) {
        super(message);
    }

    /**
     * Constructor with message and cause
     *
     * @param message Blacklist reason
     * @param cause   Root cause exception
     */
    public TokenBlacklistedException(String message, Throwable cause) {
        super(message, cause);
    }
}