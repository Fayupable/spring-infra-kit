package io.fayupable.jwtrefreshtoken.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Refresh Token Entity
 *
 * Stores refresh tokens for JWT authentication.
 * Refresh tokens are long-lived tokens used to obtain new access tokens.
 *
 * Database Table: refresh_tokens
 *
 * Key Concepts:
 *
 * 1. Access Token vs Refresh Token:
 *    - Access Token: Short-lived (15 min), contains user info, sent with every request
 *    - Refresh Token: Long-lived (30 days), used only to get new access token
 *
 * 2. Why Store Refresh Tokens?
 *    - Enables token revocation (logout across devices)
 *    - Tracks active sessions per user
 *    - Implements security policies (max tokens per user)
 *    - Audit trail of authentication events
 *
 * 3. Token Rotation:
 *    - Each refresh generates a NEW refresh token
 *    - Old token is marked as replaced (replacedBy field)
 *    - Prevents replay attacks
 *
 * 4. Security Features:
 *    - Token hash stored (not plain token)
 *    - Device and IP tracking
 *    - Absolute expiry (maxExpiry)
 *    - Sliding window expiry (expiresAt)
 *    - Revocation support
 *
 * Design Decision:
 * - Uses userId (UUID) instead of @ManyToOne relationship
 * - More flexible and performant
 * - No unnecessary JPA joins
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "refresh_tokens",
        indexes = {
                // Fast lookup by user (e.g., get all user's tokens for logout)
                @Index(name = "idx_refresh_tokens_user_id", columnList = "user_id"),
                // Fast validation during token refresh
                @Index(name = "idx_refresh_tokens_token_hash", columnList = "token_hash"),
                // Efficient cleanup of expired tokens
                @Index(name = "idx_refresh_tokens_expires_at", columnList = "expires_at")
        }
)
public class RefreshToken {

    /**
     * Primary Key
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "refresh_token_id")
    private UUID refreshTokenId;

    /**
     * User Reference
     *
     * Stores user ID directly without JPA relationship.
     *
     * Why not @ManyToOne?
     * - Simpler design: no bidirectional relationship complexity
     * - Better performance: no automatic JOIN when loading tokens
     * - More flexible: RefreshToken doesn't need User entity
     * - Decoupled: Changes to User entity don't affect RefreshToken
     *
     * Database foreign key still exists for referential integrity.
     */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * Hashed Token
     *
     * CRITICAL: We store the HASH of the refresh token, not the token itself.
     *
     * Why hash the token?
     * - If database is compromised, attacker can't use tokens directly
     * - Similar to password hashing
     * - We use SHA-256 for hashing (fast, one-way)
     *
     * Flow:
     * 1. Generate random token (UUID or secure random string)
     * 2. Hash it with SHA-256
     * 3. Store hash in database
     * 4. Return plain token to user
     * 5. On refresh: hash incoming token and compare with stored hash
     *
     * Length 64: SHA-256 produces 64 character hex string
     */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    /**
     * Token Issue Time
     *
     * When this token was created.
     * Used for calculating token age and tracking.
     */
    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    /**
     * Sliding Window Expiry
     *
     * This timestamp extends each time token is used (sliding window).
     *
     * Example:
     * - Token created: Jan 1, expires_at: Jan 31 (30 days)
     * - Token used: Jan 15, expires_at: Feb 14 (30 days from use)
     * - Token used: Feb 10, expires_at: Mar 12 (30 days from use)
     *
     * Why sliding window?
     * - Active users don't need to re-login
     * - Inactive tokens auto-expire
     * - Better UX for frequently used apps
     *
     * BUT: Limited by maxExpiry (absolute limit)
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * Absolute Maximum Expiry
     *
     * Hard limit for token lifetime, regardless of usage.
     *
     * Example:
     * - Token created: Jan 1
     * - maxExpiry: Apr 1 (90 days absolute max)
     * - Even if used daily, token MUST expire on Apr 1
     *
     * Why absolute limit?
     * - Security: forces periodic re-authentication
     * - Compliance: meets security policies (e.g., PCI-DSS)
     * - Credential rotation best practice
     *
     * Typical values:
     * - expiresAt: 30 days (sliding)
     * - maxExpiry: 90 days (absolute)
     */
    @Column(name = "max_expiry", nullable = false)
    private LocalDateTime maxExpiry;

    /**
     * Revocation Flag
     *
     * Marks token as revoked (invalidated).
     *
     * Why revoke instead of delete?
     * - Audit trail: keeps record of revoked tokens
     * - Security: can detect replay attacks
     * - Analytics: track logout patterns
     *
     * When to revoke:
     * - User explicitly logs out
     * - User changes password
     * - Admin suspends account
     * - Security breach detected
     * - Token reuse detected (replay attack)
     */
    @Column(name = "revoked", nullable = false)
    @Builder.Default
    private Boolean revoked = false;

    /**
     * Token Rotation Chain
     *
     * When token is refreshed, new token is created and old token is replaced.
     * This field stores the ID of the new token that replaced this one.
     *
     * Why track replacement?
     * - Detect token reuse attacks (critical security feature)
     * - Build token family tree for audit
     * - Revoke entire token family if needed
     *
     * Token Rotation Flow:
     * 1. User refreshes with Token A
     * 2. Create Token B
     * 3. Set Token A's replacedBy = Token B's ID
     * 4. Mark Token A as revoked
     * 5. Return Token B to user
     *
     * Replay Attack Detection:
     * If Token A is used again after being replaced:
     * - It's a replay attack!
     * - Revoke entire family (Token A, B, and any descendants)
     * - Force user to re-authenticate
     *
     * Example Chain:
     * Token A (replaced, replacedBy = B) → Token B (replaced, replacedBy = C) → Token C (active)
     */
    @Column(name = "replaced_by")
    private UUID replacedBy;

    /**
     * Device Information
     *
     * Stores device details for security and UX.
     *
     * Typical content:
     * - User agent string
     * - Device type (mobile, desktop, tablet)
     * - OS and browser
     *
     * Uses:
     * - Show "Active sessions" to user (like Google/Facebook)
     * - Detect suspicious login from new device
     * - Send security alerts
     * - Analytics
     *
     * Example: "Chrome 120.0.0.0 on Windows 11"
     */
    @Column(name = "device_info", length = 500)
    private String deviceInfo;

    /**
     * IP Address
     *
     * IP address from which token was created.
     *
     * Uses:
     * - Security monitoring
     * - Detect suspicious activity (login from different country)
     * - Geolocation tracking
     * - Rate limiting per IP
     *
     * Length 45: Supports both IPv4 (15 chars) and IPv6 (45 chars)
     * Example IPv4: "192.168.1.1"
     * Example IPv6: "2001:0db8:85a3:0000:0000:8a2e:0370:7334"
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * Audit Timestamps
     *
     * Track when token was created and last updated.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * JPA Lifecycle Callbacks
     */
    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Helper Method: Check if token is expired
     *
     * Token is expired if:
     * - Current time > expiresAt (sliding window expired)
     * - OR current time > maxExpiry (absolute limit reached)
     *
     * @return true if token is expired, false otherwise
     */
    public boolean isExpired() {
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(expiresAt) || now.isAfter(maxExpiry);
    }

    /**
     * Helper Method: Check if token is valid
     *
     * Token is valid if:
     * - Not expired (checks both sliding and absolute expiry)
     * - Not revoked (user hasn't logged out or been suspended)
     *
     * @return true if token can be used, false otherwise
     */
    public boolean isValid() {
        return !isExpired() && !revoked;
    }
}