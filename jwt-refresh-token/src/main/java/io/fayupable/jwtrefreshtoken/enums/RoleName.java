package io.fayupable.jwtrefreshtoken.enums;

/**
 * System Role Names Enum
 * <p>
 * Defines the authorization roles in the system.
 * Used for role-based access control (RBAC).
 * <p>
 * Role Hierarchy:
 * ROLE_ADMIN > ROLE_USER
 * <p>
 * Note: Spring Security requires "ROLE_" prefix by convention.
 */
public enum RoleName {
    /**
     * Regular user role
     * Default role assigned to all registered users
     * Basic system access
     */
    ROLE_USER,

    /**
     * Administrator role
     * Full system access including user management
     * Inherits ROLE_USER permissions
     */
    ROLE_ADMIN
}