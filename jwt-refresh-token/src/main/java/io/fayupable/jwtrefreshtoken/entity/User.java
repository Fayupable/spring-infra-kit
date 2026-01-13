package io.fayupable.jwtrefreshtoken.entity;

import io.fayupable.jwtrefreshtoken.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * User Entity
 *
 * Core entity representing a user account in the system.
 * Handles authentication (who you are) via email/password.
 * Handles authorization (what you can do) via roles.
 *
 * Database Table: users
 *
 * Key Features:
 * - Email-based authentication
 * - BCrypt password hashing
 * - Role-based access control
 * - Account status management
 * - Audit timestamps
 *
 * Relationships:
 * - Many-to-Many with Role (via user_roles junction table)
 * - RefreshTokens accessed via userId (no JPA relationship)
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "users",
        uniqueConstraints = {
                // Email must be unique (used for login)
                @UniqueConstraint(name = "uk_users_email", columnNames = "email")
        },
        indexes = {
                // Fast lookup during login
                @Index(name = "idx_users_email", columnList = "email"),
                // Fast filtering by status (e.g., find all active users)
                @Index(name = "idx_users_status", columnList = "status"),
                // Fast sorting by creation date
                @Index(name = "idx_users_created_at", columnList = "created_at")
        }
)
public class User {

    /**
     * Primary Key
     * UUID for security and distributed system compatibility
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id")
    private UUID userId;

    /**
     * Email Address
     *
     * Used as the username for authentication.
     * Must be unique across the system.
     *
     * Why email as username?
     * - Users remember their email
     * - Naturally unique
     * - Easy password reset flow
     */
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    /**
     * Username (Display Name)
     *
     * Optional field for display purposes.
     * Can be non-unique if needed for flexibility.
     *
     * If not provided, defaults to email in service layer.
     */
    @Column(name = "username", length = 30)
    private String username;

    /**
     * Password Hash
     *
     * NEVER store plain text passwords!
     * This field stores BCrypt hashed passwords.
     *
     * BCrypt hash format: $2a$10$[salt][hash]
     * Length: 60 characters
     *
     * Why BCrypt?
     * - Industry standard for password hashing
     * - Built-in salting (prevents rainbow table attacks)
     * - Computationally expensive (slows down brute force)
     * - Adjustable cost factor (future-proof)
     *
     * Why nullable = true?
     * - Future support for OAuth-only accounts (Google, etc.)
     * - These accounts don't have passwords
     */
    @Column(name = "password_hash", nullable = true, length = 60)
    private String passwordHash;

    /**
     * Account Status
     *
     * Controls user's ability to access the system.
     *
     * PENDING_APPROVAL: Cannot login (needs verification)
     * ACTIVE: Full access
     * SUSPENDED: Temporarily blocked
     * BANNED: Permanently blocked
     *
     * Default: PENDING_APPROVAL (set in @PrePersist)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus userStatus;

    /**
     * User Roles
     *
     * Many-to-Many relationship with Role entity.
     * Junction table: user_roles
     *
     * Why EAGER fetch?
     * - Roles are needed during every authentication
     * - Small dataset (typically 1-3 roles per user)
     * - Avoids LazyInitializationException in SecurityContext
     *
     * Why CascadeType.PERSIST and MERGE only?
     * - We don't want to delete roles when user is deleted
     * - Roles are shared across users
     * - Only want to sync the relationship, not the roles themselves
     */
    @ManyToMany(
            cascade = {CascadeType.PERSIST, CascadeType.MERGE},
            fetch = FetchType.EAGER
    )
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(
                    name = "user_id",
                    foreignKey = @ForeignKey(name = "fk_user_roles_user_id")
            ),
            inverseJoinColumns = @JoinColumn(
                    name = "role_id",
                    foreignKey = @ForeignKey(name = "fk_user_roles_role_id")
            ),
            uniqueConstraints = @UniqueConstraint(
                    name = "uk_user_roles",
                    columnNames = {"user_id", "role_id"}
            )
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    /**
     * Account Creation Timestamp
     *
     * Automatically set when entity is first persisted.
     * updatable = false: prevents accidental modification
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Last Update Timestamp
     *
     * Automatically updated whenever entity is modified.
     * Useful for audit trails and cache invalidation.
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Last Login Timestamp
     *
     * Tracks when user last successfully logged in.
     * Useful for:
     * - Security monitoring (detect unusual activity)
     * - User engagement metrics
     * - Account cleanup (delete inactive accounts)
     */
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    /**
     * JPA Lifecycle Callback: Before Persist
     *
     * Automatically called before entity is saved for the first time.
     * Sets timestamps and default values.
     */
    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;

        // Ensure status is never null
        if (userStatus == null) {
            userStatus = UserStatus.PENDING_APPROVAL;
        }
    }

    /**
     * JPA Lifecycle Callback: Before Update
     *
     * Automatically called before entity is updated.
     * Updates the timestamp.
     */
    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Helper Method: Update Last Login
     *
     * Called after successful authentication.
     * Updates lastLoginAt timestamp.
     */
    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }

    /**
     * Helper Method: Add Role
     *
     * Convenience method to add a role to user.
     * Maintains relationship consistency.
     */
    public void addRole(Role role) {
        this.roles.add(role);
    }
}