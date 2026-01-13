package io.fayupable.jwtbasic.enums;

/**
 * User account statuses
 * <p>
 * PENDING_APPROVAL: Waiting for email verification
 * ACTIVE: Active user (can access the system)
 * SUSPENDED: Temporarily suspended
 * BANNED: Permanently banned
 */
public enum UserStatus {
    PENDING_APPROVAL,  // Waiting for email verification
    ACTIVE,            // Active user
    SUSPENDED,         // Temporary ban
    BANNED            // Permanent ban
}