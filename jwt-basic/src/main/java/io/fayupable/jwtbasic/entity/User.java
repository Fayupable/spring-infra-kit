package io.fayupable.jwtbasic.entity;

import io.fayupable.jwtbasic.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * User Entity - User information
 * <p>
 * In this simple JWT module, we only keep the essential
 * fields needed for basic authentication.
 * <p>
 * Stored in "users" table in the database.
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
                // Email must be unique
                @UniqueConstraint(name = "uk_users_email", columnNames = "email")
        },
        indexes = {
                // Index for fast lookup by email
                @Index(name = "idx_users_email", columnList = "email"),
                // Index for filtering by status
                @Index(name = "idx_users_status", columnList = "status")
        }
)
public class User {

    /**
     * Primary key - Using UUID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id")
    private UUID id;

    /**
     * Email - Used for login
     * Must be unique and non-null
     */
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    /**
     * Password Hash - BCrypt hashed password
     * <p>
     * NOTE: Plain text passwords are NEVER stored in database!
     * BCrypt hash is 60 characters, so length=60 is sufficient
     */
    @Column(name = "password_hash", nullable = false, length = 60)
    private String passwordHash;

    /**
     * Username - Optional field
     */
    @Column(name = "username", length = 50)
    private String username;

    /**
     * User status
     * <p>
     * PENDING_APPROVAL: New registration, not yet active
     * ACTIVE: Active user
     * SUSPENDED: Temporary ban
     * BANNED: Permanent ban
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private UserStatus status = UserStatus.PENDING_APPROVAL;

    /**
     * User roles - Many-to-Many relationship
     * <p>
     * A user can have multiple roles
     * Example: [ROLE_USER, ROLE_ADMIN]
     * <p>
     * We use EAGER fetch because we need roles during authentication
     */
    @ManyToMany(fetch = FetchType.EAGER)
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
     * Account creation timestamp
     * Automatically set by @PrePersist
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Last update timestamp
     * Automatically updated by @PreUpdate
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Runs before the entity is first persisted
     * Automatically sets createdAt and updatedAt
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    /**
     * Runs every time the entity is updated
     * Automatically updates updatedAt
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Helper method: Add role to user
     */
    public void addRole(Role role) {
        this.roles.add(role);
    }
}