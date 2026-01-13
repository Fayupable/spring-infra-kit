package io.fayupable.jwtrefreshtoken.exception;

import org.springframework.security.authentication.LockedException;

/**
 * Account Locked Exception
 * <p>
 * Thrown when user account is locked/banned.
 * <p>
 * Scenarios:
 * - User status: BANNED (permanent lock)
 * - User status: SUSPENDED (temporary lock)
 * - Too many failed login attempts
 * - Terms of service violation
 * - Admin banned user
 * - Security incident detected
 * <p>
 * HTTP Response:
 * - Status: 403 Forbidden (not 401!)
 * - Message: "Account is locked. Contact support."
 * <p>
 * Why 403 not 401?
 * - 401: Authentication failed (wrong credentials)
 * - 403: Authenticated but not authorized (account locked)
 * <p>
 * Client Action:
 * - Show "Account locked" message
 * - Provide support contact info
 * - Don't allow login attempts
 * - Redirect to support page
 * <p>
 * Lock Types:
 * - BANNED: Permanent (terms violation, fraud)
 * - SUSPENDED: Temporary (suspicious activity, investigation)
 * <p>
 * User Status Mapping:
 * - ACTIVE → Allowed
 * - PENDING_APPROVAL → Disabled (AccountDisabledException)
 * - BANNED → Locked (this exception)
 * - SUSPENDED → Locked (this exception)
 * <p>
 * Checked In:
 * - JwtAuthenticationFilter (on every request)
 * - UserDetailsImpl.isAccountNonLocked() method
 * <p>
 * Security Note:
 * Prevents banned/suspended users from accessing system.
 * Even with valid credentials, they cannot login.
 * <p>
 * Admin Features:
 * - Ban reason stored in database
 * - Ban expiry date (for suspensions)
 * - Unban option for admins
 * - Audit log of lock/unlock events
 */
public class AccountLockedException extends LockedException {

    /**
     * Constructor with message
     *
     * @param message Lock reason (e.g., "Account banned for terms violation")
     */
    public AccountLockedException(String message) {
        super(message);
    }

    /**
     * Constructor with message and cause
     *
     * @param message Lock reason
     * @param cause   Root cause exception
     */
    public AccountLockedException(String message, Throwable cause) {
        super(message, cause);
    }
}
