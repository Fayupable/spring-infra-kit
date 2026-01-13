package io.fayupable.jwtrefreshtoken.enums;

/**
 * User Account Status Enum
 * <p>
 * Defines the lifecycle states of a user account in the system.
 * <p>
 * Status Flow:
 * PENDING_APPROVAL → ACTIVE → SUSPENDED/BANNED
 * <p>
 * PENDING_APPROVAL: New user, awaiting email verification
 * ACTIVE: Verified user, full system access
 * SUSPENDED: Temporarily restricted (can be reactivated)
 * BANNED: Permanently restricted (cannot login)
 */
public enum UserStatus {
    /**
     * User registered but not yet verified
     * Cannot login until email is confirmed
     */
    PENDING_APPROVAL,

    /**
     * Verified and active user
     * Full access to system features
     */
    ACTIVE,

    /**
     * Temporarily suspended account
     * Can be reactivated by admin
     */
    SUSPENDED,

    /**
     * Permanently banned account
     * Cannot be reactivated
     */
    BANNED
}