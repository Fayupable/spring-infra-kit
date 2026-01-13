package io.fayupable.jwtrefreshtoken.service.cookie.blacklist;

/**
 * Token Blacklist Service Interface
 * <p>
 * Manages blacklisting of invalidated access tokens.
 * <p>
 * Why Blacklist Access Tokens?
 * - Access tokens are stateless (JWT)
 * - They don't check database on every request
 * - Valid until expiration (15 minutes)
 * - Blacklisting prevents immediate reuse after logout
 * <p>
 * Use Cases:
 * - User logout (revoke current access token)
 * - Password change (revoke all active sessions)
 * - Security breach detected (emergency revocation)
 * - Admin bans user (invalidate all tokens)
 * <p>
 * Implementation Strategy:
 * - Primary: Redis (fast, distributed, automatic expiry)
 * - Fallback: In-Memory (local, scheduled cleanup)
 * <p>
 * Spring Boot Conditional Loading:
 * - If Redis available → RedisTokenBlacklistService
 * - If Redis unavailable → InMemoryTokenBlacklistService
 * <p>
 * Interface Benefits:
 * - Easy to swap implementations
 * - Clean dependency injection
 * - Testable (mock interface)
 * - Future-proof (add new implementations)
 */
public interface TokenBlacklistService {

    /**
     * Add token to blacklist
     * <p>
     * Blacklists an access token for remaining lifetime.
     * <p>
     * Process:
     * 1. Extract token
     * 2. Calculate remaining TTL (time to live)
     * 3. Store in blacklist with TTL
     * 4. Token expires automatically after TTL
     * <p>
     * Why TTL?
     * - Token expires anyway after 15 minutes
     * - No need to keep it forever
     * - Saves storage space
     * - Automatic cleanup
     * <p>
     * Storage Format:
     * - Redis: Key = "blacklist:TOKEN", Value = "1", TTL = remaining seconds
     * - In-Memory: Map<String, Instant> with scheduled cleanup
     * <p>
     * Called During:
     * - User logout
     * - Password change
     * - Admin bans user
     * - Security breach detected
     * <p>
     * Example:
     * tokenBlacklistService.blacklistToken(
     * "eyJhbGciOiJIUzI1NiIs...",  // access token
     * 900                          // TTL in seconds (15 min)
     * );
     *
     * @param token      Access token to blacklist (JWT string)
     * @param ttlSeconds Time to live in seconds (usually 900 = 15 min)
     */
    void blacklistToken(String token, long ttlSeconds);

    /**
     * Check if token is blacklisted
     * <p>
     * Verifies if token has been revoked.
     * <p>
     * Process:
     * 1. Look up token in blacklist
     * 2. Return true if found
     * 3. Return false if not found or expired
     * <p>
     * Called During:
     * - Every API request (in JwtAuthenticationFilter)
     * - Before setting authentication
     * - Before processing request
     * <p>
     * Performance:
     * - Redis: O(1) lookup (very fast)
     * - In-Memory: O(1) HashMap lookup (fast)
     * <p>
     * Example:
     * if (tokenBlacklistService.isBlacklisted("eyJhbGci...")) {
     * throw new UnauthorizedException("Token revoked");
     * }
     *
     * @param token Access token to check
     * @return true if token is blacklisted, false otherwise
     */
    boolean isBlacklisted(String token);

    /**
     * Remove token from blacklist
     * <p>
     * Manually removes a token from blacklist.
     * <p>
     * Use Cases:
     * - Admin reinstates user (rare)
     * - Testing/debugging
     * - Manual cleanup
     * <p>
     * Note: Usually not needed.
     * Tokens expire automatically via TTL.
     *
     * @param token Access token to remove
     */
    void removeToken(String token);

    /**
     * Clear all blacklisted tokens
     * <p>
     * Removes all tokens from blacklist.
     * <p>
     * Use Cases:
     * - Testing/debugging
     * - System maintenance
     * - Emergency cleanup
     * <p>
     * Warning: Use with caution in production!
     * This will allow all blacklisted tokens to work again.
     * <p>
     * Implementation:
     * - Redis: Delete all keys matching "blacklist:*"
     * - In-Memory: Clear the map
     */
    void clearAll();

    /**
     * Get total blacklisted tokens count
     * <p>
     * Returns number of currently blacklisted tokens.
     * <p>
     * Use Cases:
     * - Monitoring/metrics
     * - Health checks
     * - Admin dashboard
     * - Debugging
     * <p>
     * Implementation:
     * - Redis: Count keys matching "blacklist:*"
     * - In-Memory: Map size
     *
     * @return Number of blacklisted tokens
     */
    long getBlacklistedCount();
}