package io.fayupable.jwtrefreshtoken.entity;

import io.fayupable.jwtrefreshtoken.enums.RoleName;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Role Entity
 *
 * Represents user roles in the system for authorization purposes.
 * Uses enum-based role names for type safety.
 *
 * Database Table: roles
 *
 * Relationships:
 * - Many-to-Many with User (via user_roles junction table)
 *
 * Why UUID for ID?
 * - Distributed system friendly (no auto-increment conflicts)
 * - Security: IDs are not predictable
 * - Easy to replicate across environments
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "roles",
        uniqueConstraints = {
                // Ensure each role name exists only once
                @UniqueConstraint(name = "uk_roles_role_name", columnNames = "role_name")
        },
        indexes = {
                // Index for faster role lookups during authorization
                @Index(name = "idx_roles_role_name", columnList = "role_name")
        }
)
public class Role {

    /**
     * Primary Key
     *
     * Using UUID instead of Long:
     * - Prevents ID enumeration attacks
     * - Works well in distributed systems
     * - No database-specific auto-increment dependency
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "role_id")
    private UUID roleId;

    /**
     * Role Name
     *
     * Using EnumType.STRING instead of ORDINAL:
     * - ORDINAL stores integer position (0, 1, 2...)
     * - STRING stores actual name ("ROLE_USER", "ROLE_ADMIN")
     * - STRING is safer: enum order changes won't break database
     * - STRING is more readable in database queries
     *
     * Why nullable = false?
     * - Role without a name is meaningless
     * - Database-level constraint prevents bad data
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role_name", nullable = false, unique = true, length = 30)
    private RoleName roleName;
}