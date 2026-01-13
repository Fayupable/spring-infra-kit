package io.fayupable.jwtbasic.entity;

import io.fayupable.jwtbasic.enums.RoleName;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Role Entity - User roles
 * <p>
 * Each user can have one or more roles.
 * Example: ROLE_USER, ROLE_ADMIN
 * <p>
 * Stored in "roles" table in the database.
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
                // Prevent duplicate roles
                @UniqueConstraint(name = "uk_roles_name", columnNames = "name")
        },
        indexes = {
                // Index for fast lookup by role name
                @Index(name = "idx_roles_name", columnList = "name")
        }
)
public class Role {

    /**
     * Primary key - Using UUID
     * <p>
     * Benefits of UUID:
     * - No collision risk in distributed systems
     * - Security: IDs are unpredictable
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "role_id")
    private UUID id;

    /**
     * Role name - Stored as enum
     * <p>
     * We use EnumType.STRING because:
     * - ORDINAL breaks if enum order changes
     * - STRING is more readable and safer
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "name", nullable = false, unique = true, length = 30)
    private RoleName name;
}