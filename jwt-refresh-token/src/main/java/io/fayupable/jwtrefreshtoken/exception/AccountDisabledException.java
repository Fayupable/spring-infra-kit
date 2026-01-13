package io.fayupable.jwtrefreshtoken.exception;

import org.springframework.security.authentication.DisabledException;

/**
 * Account Disabled Exception
 * <p>
 * Thrown when user account is disabled/inactive.
 * <p>
 * Scenarios:
 * - User status: PENDING_APPROVAL (email not verified)
 * - User status: INACTIVE (account deactivated)
 * - Email verification required but not completed
 * - Account pending admin approval
 * <p>
 * HTTP Response:
 * - Status: 403 Forbidden (not 401!)
 * - Message: "Account is disabled. Please verify your email."
 * <p>
 * Why 403 not 401?
 * - 401: Authentication failed (wrong credentials)
 * - 403: Authenticated but not authorized (account disabled)
 * <p>
 * Client Action:
 * - Show "Account disabled" message
 * - Redirect to email verification page
 * - Or show "Contact support" message
 * <p>
 * User Status Mapping:
 * - ACTIVE → Allowed
 * - PENDING_APPROVAL → Disabled  (this exception)
 * - BANNED → Locked  (AccountLockedException)
 * - SUSPENDED → Locked  (AccountLockedException)
 * <p>
 * Checked In:
 * - JwtAuthenticationFilter (on every request)
 * - UserDetailsImpl.isEnabled() method
 * <p>
 * Security Note:
 * Prevents unverified accounts from accessing system.
 * Critical for email verification flow.
 */
public class AccountDisabledException extends DisabledException {

    /**
     * Constructor with message
     *
     * @param message Disable reason (e.g., "Email not verified")
     */
    public AccountDisabledException(String message) {
        super(message);
    }

    /**
     * Constructor with message and cause
     *
     * @param message Disable reason
     * @param cause   Root cause exception
     */
    public AccountDisabledException(String message, Throwable cause) {
        super(message, cause);
    }
}