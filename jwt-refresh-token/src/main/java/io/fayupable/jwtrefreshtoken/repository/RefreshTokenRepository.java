package io.fayupable.jwtrefreshtoken.repository;

import io.fayupable.jwtrefreshtoken.entity.RefreshToken;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Refresh Token Repository
 * <p>
 * Data access layer for RefreshToken entity.
 * Provides methods to manage refresh tokens in database.
 * <p>
 * Key Operations:
 * - Token validation
 * - Token rotation
 * - Token revocation
 * - Expired token cleanup
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Find valid (non-revoked) token by hash
     * <p>
     * Primary method for token validation during refresh.
     * Only returns tokens that haven't been revoked.
     * <p>
     * Used during:
     * - Token refresh request
     * - Token validation
     *
     * @param tokenHash SHA-256 hash of the token
     * @return Optional containing token if found and not revoked, empty otherwise
     * <p>
     * Example:
     * String hash = hashToken(incomingToken);
     * Optional<RefreshToken> token = repository.findByTokenHashAndRevokedFalse(hash);
     * <p>
     * Query: SELECT * FROM refresh_tokens WHERE token_hash = ? AND revoked = false
     */
    Optional<RefreshToken> findByTokenHashAndRevokedFalse(String tokenHash);

    /**
     * Find latest active token for a user
     * <p>
     * Returns the most recently created non-revoked token for a user.
     * Useful for checking if user has any active sessions.
     *
     * @param userId User's unique identifier
     * @return Optional containing latest active token, empty if none found
     * <p>
     * Example:
     * Optional<RefreshToken> latestToken = repository.findLatestActiveTokenByUserId(userId);
     * <p>
     * Query: Custom JPQL with ORDER BY and LIMIT
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.userId = :userId AND rt.revoked = false ORDER BY rt.createdAt DESC")
    Optional<RefreshToken> findLatestActiveTokenByUserId(@Param("userId") UUID userId);

    /**
     * Get all token IDs for a user (paginated)
     * <p>
     * Returns only IDs (not full entities) for memory efficiency.
     * Useful for bulk operations like revocation.
     *
     * @param userId   User's unique identifier
     * @param pageable Pagination parameters
     * @return List of token IDs
     * <p>
     * Example:
     * List<UUID> tokenIds = repository.findTokenIdsByUserId(userId, PageRequest.of(0, 100));
     * <p>
     * Query: SELECT refresh_token_id FROM refresh_tokens WHERE user_id = ?
     */
    @Query("SELECT rt.refreshTokenId FROM RefreshToken rt WHERE rt.userId = :userId")
    List<UUID> findTokenIdsByUserId(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Find all active tokens for a user
     * <p>
     * Used to display active sessions to user or revoke all sessions.
     *
     * @param userId User's unique identifier
     * @return List of active tokens
     * <p>
     * Example:
     * List<RefreshToken> sessions = repository.findByUserIdAndRevokedFalse(userId);
     * // Show user: "You have 3 active sessions"
     * <p>
     * Query: SELECT * FROM refresh_tokens WHERE user_id = ? AND revoked = false
     */
    List<RefreshToken> findByUserIdAndRevokedFalse(UUID userId);

    /**
     * Revoke all tokens for a user
     * <p>
     * Bulk update operation to invalidate all user's tokens.
     * <p>
     * Used during:
     * - "Logout from all devices" feature
     * - Password change (security: invalidate all sessions)
     * - Account suspension
     *
     * @param userId User's unique identifier
     *               <p>
     *               Example:
     *               repository.revokeAllUserTokens(userId);
     *               <p>
     *               Query: UPDATE refresh_tokens SET revoked = true WHERE user_id = ?
     *               <p>
     *               Note: @Modifying required for UPDATE/DELETE queries
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.userId = :userId")
    void revokeAllUserTokens(@Param("userId") UUID userId);

    /**
     * Delete expired tokens (cleanup)
     * <p>
     * Bulk delete operation to remove expired tokens.
     * Should be called periodically (e.g., daily scheduled task).
     *
     * @param now Current timestamp
     *            <p>
     *            Example:
     *            repository.deleteExpiredTokens(LocalDateTime.now());
     *            <p>
     *            Query: DELETE FROM refresh_tokens WHERE expires_at < ? OR max_expiry < ?
     *            <p>
     *            Note: @Modifying required for DELETE queries
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now OR rt.maxExpiry < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);

    /**
     * Find expired or old revoked token IDs (for batch deletion)
     * <p>
     * Returns IDs of tokens that are:
     * - Expired (either sliding window or absolute expiry)
     * - Revoked AND updated more than 24 hours ago
     * <p>
     * Paginated to avoid memory issues with large datasets.
     *
     * @param now      Current timestamp
     * @param cutoff   Timestamp for old revoked tokens (e.g., 24 hours ago)
     * @param pageable Pagination parameters
     * @return List of token IDs to delete
     * <p>
     * Example:
     * LocalDateTime now = LocalDateTime.now();
     * LocalDateTime cutoff = now.minusDays(1);
     * List<UUID> ids = repository.findExpiredOrRevokedTokenIds(now, cutoff, PageRequest.of(0, 1000));
     * repository.deleteByIds(ids);
     * <p>
     * Why keep revoked tokens for 24 hours?
     * - Detect replay attacks (reuse of revoked token)
     * - Audit trail for recent logouts
     * - Grace period for investigating security issues
     */
    @Query("SELECT rt.refreshTokenId FROM RefreshToken rt " +
            "WHERE rt.expiresAt < :now " +
            "OR rt.maxExpiry < :now " +
            "OR (rt.revoked = true AND rt.updatedAt < :cutoff)")
    List<UUID> findExpiredOrRevokedTokenIds(
            @Param("now") LocalDateTime now,
            @Param("cutoff") LocalDateTime cutoff,
            Pageable pageable
    );

    /**
     * Delete tokens by IDs (batch deletion)
     * <p>
     * Deletes multiple tokens efficiently in a single query.
     * Used with findExpiredOrRevokedTokenIds for cleanup.
     *
     * @param ids List of token IDs to delete
     * @return Number of tokens deleted
     * <p>
     * Example:
     * List<UUID> idsToDelete = findExpiredOrRevokedTokenIds(...);
     * int deleted = repository.deleteByIds(idsToDelete);
     * log.info("Deleted {} expired tokens", deleted);
     * <p>
     * Query: DELETE FROM refresh_tokens WHERE refresh_token_id IN (...)
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.refreshTokenId IN :ids")
    int deleteByIds(@Param("ids") List<UUID> ids);

    /**
     * Count active tokens for a user
     * <p>
     * Used to enforce limits (e.g., max 5 devices per user).
     * More efficient than loading all tokens and counting.
     *
     * @param userId User's unique identifier
     * @return Number of active (non-revoked) tokens
     * <p>
     * Example:
     * long activeTokens = repository.countActiveTokensByUserId(userId);
     * if (activeTokens >= 5) {
     * throw new RuntimeException("Maximum devices reached");
     * }
     * <p>
     * Query: SELECT COUNT(*) FROM refresh_tokens WHERE user_id = ? AND revoked = false
     */
    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.userId = :userId AND rt.revoked = false")
    long countActiveTokensByUserId(@Param("userId") UUID userId);

    /**
     * Delete all tokens for a user
     * <p>
     * Hard delete (not revoke) all user's tokens.
     * <p>
     * Used during:
     * - Account deletion
     * - Data cleanup
     *
     * @param userId User's unique identifier
     *               <p>
     *               Example:
     *               repository.deleteByUserId(userId);
     *               <p>
     *               Query: DELETE FROM refresh_tokens WHERE user_id = ?
     *               <p>
     *               Note: Spring Data JPA automatically generates this method
     */
    void deleteByUserId(UUID userId);
}