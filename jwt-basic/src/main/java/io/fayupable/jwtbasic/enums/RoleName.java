package io.fayupable.jwtbasic.enums;

/**
 * System roles
 *
 * We use this enum to store roles in the database.
 * Using enum instead of String is more type-safe.
 */
public enum RoleName {
    ROLE_USER,   // Regular user
    ROLE_ADMIN   // Admin user
}