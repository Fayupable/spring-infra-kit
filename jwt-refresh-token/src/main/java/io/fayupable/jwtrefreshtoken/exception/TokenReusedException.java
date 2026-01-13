package io.fayupable.jwtrefreshtoken.exception;

import org.springframework.security.core.AuthenticationException;

/**
 * Token Reuse Exception
 * <p>
 * Thrown when a revoked/rotated refresh token is reused.
 * <p>
 * This is a CRITICAL SECURITY EVENT!
 * <p>
 * Attack Scenario:
 * 1. Attacker steals refresh token
 * 2. Legitimate user refreshes (token rotated)
 * 3. Attacker tries to use old token (REUSE DETECTED!)
 * 4. System revokes ALL user tokens
 * 5. Both attacker and user logged out
 * <p>
 * Detection:
 * - Token marked as revoked in database
 * - Token has "replacedBy" field set
 * - Attempt to use old token after rotation
 * <p>
 * Response:
 * - Revoke entire token family (all user sessions)
 * - Force user to re-login
 * - Log security event for investigation
 * - Alert security team (production)
 * <p>
 * HTTP Response:
 * - Status: 401 Unauthorized
 * - Message: "Security breach detected. All sessions revoked."
 * <p>
 * Client Action:
 * - Clear all tokens
 * - Redirect to login page
 * - Show security warning
 * <p>
 * Token Family Example:
 * Token A (original) → Token B (rotation 1) → Token C (rotation 2)
 * If Token A reused → Revoke A, B, C (entire family)
 * <p>
 * Security Best Practice:
 * This is token rotation replay attack detection.
 * Industry standard for OAuth 2.0 security.
 */
public class TokenReusedException extends AuthenticationException {

    /**
     * Constructor with message
     *
     * @param message Reuse detection details
     */
    public TokenReusedException(String message) {
        super(message);
    }

    /**
     * Constructor with message and cause
     *
     * @param message Reuse detection details
     * @param cause   Root cause exception
     */
    public TokenReusedException(String message, Throwable cause) {
        super(message, cause);
    }
}