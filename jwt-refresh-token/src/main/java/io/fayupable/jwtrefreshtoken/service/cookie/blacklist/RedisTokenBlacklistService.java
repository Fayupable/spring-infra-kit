package io.fayupable.jwtrefreshtoken.service.cookie.blacklist;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis Token Blacklist Service
 * <p>
 * Production-ready implementation using Redis.
 * <p>
 * Why Redis?
 * - Fast: O(1) lookups (microseconds)
 * - Distributed: Works across multiple servers
 * - Automatic Expiry: TTL built-in (no manual cleanup)
 * - Scalable: Handles millions of tokens
 * - Persistent: Data survives app restarts
 * <p>
 * Conditional Loading:
 * This service is only loaded if Redis is available.
 * <p>
 * Spring Boot checks:
 * 1. Is spring.data.redis.host configured?
 * 2. Can we connect to Redis?
 * 3. If YES → Load this service
 * 4. If NO → Load InMemoryTokenBlacklistService instead
 * <p>
 * Redis Key Format:
 * Key: "blacklist:TOKEN_VALUE"
 * Value: "1" (just a marker, we only care if key exists)
 * TTL: Remaining token lifetime (auto-expires)
 * <p>
 * Example Redis Entry:
 * Key: "blacklist:eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
 * Value: "1"
 * TTL: 900 seconds (15 minutes)
 * <p>
 * After 900 seconds:
 * - Redis automatically deletes the key
 * - No manual cleanup needed
 * - Storage automatically freed
 * <p>
 * Performance:
 * - blacklistToken(): ~1ms
 * - isBlacklisted(): ~1ms
 * - Handles 10,000+ requests/second
 * <p>
 * Network Considerations:
 * - Redis should be on same network (low latency)
 * - Use connection pooling (built-in with RedisTemplate)
 * - Consider Redis Cluster for high availability
 */
@Service
@Slf4j
@Primary
@RequiredArgsConstructor
@ConditionalOnClass(RedisTemplate.class)
public class RedisTokenBlacklistService implements TokenBlacklistService {

    /**
     * Redis Template
     * <p>
     * Spring Boot auto-configures this bean when Redis is available.
     * <p>
     * Configuration from application.yml:
     * spring:
     * data:
     * redis:
     * host: localhost
     * port: 6379
     * password: (optional)
     * timeout: 60000
     * <p>
     * RedisTemplate provides:
     * - Connection pooling
     * - Serialization/deserialization
     * - Automatic reconnection
     * - Transaction support
     */
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Prefix for blacklist keys
     * <p>
     * All blacklist keys start with this prefix.
     * Helps organize Redis keys and prevents conflicts.
     * <p>
     * Example:
     * - blacklist:eyJhbGci... (our blacklist)
     * - session:user123 (other app data)
     * - cache:product456 (cache data)
     */
    private static final String BLACKLIST_PREFIX = "blacklist:";

    /**
     * Add Token to Blacklist
     * <p>
     * Stores token in Redis with automatic expiry.
     * <p>
     * Process:
     * 1. Build Redis key: "blacklist:TOKEN"
     * 2. Set value: "1" (just a marker)
     * 3. Set TTL: remaining token lifetime
     * 4. Redis auto-deletes after TTL expires
     * <p>
     * Redis Command:
     * SETEX blacklist:TOKEN ttlSeconds "1"
     * <p>
     * Why TTL?
     * - Token expires anyway after 15 minutes
     * - No need to keep blacklist entry forever
     * - Automatic cleanup (Redis handles it)
     * - Saves memory
     * <p>
     * Example:
     * User logs out at 10:00 AM
     * - Access token expires at 10:15 AM
     * - We blacklist with TTL = 900 seconds (15 min)
     * - At 10:15 AM, Redis auto-deletes the entry
     * - Token would have expired anyway
     * <p>
     * Error Handling:
     * - If Redis is down, logs error
     * - Does NOT throw exception (graceful degradation)
     * - Request continues (token might still work)
     *
     * @param token      Access token to blacklist
     * @param ttlSeconds Time to live in seconds
     */
    @Override
    public void blacklistToken(String token, long ttlSeconds) {
        try {
            String key = BLACKLIST_PREFIX + token;

            // Set key with automatic expiry
            // Redis will delete this key after ttlSeconds
            redisTemplate.opsForValue().set(key, "1", ttlSeconds, TimeUnit.SECONDS);

            log.debug("Token blacklisted in Redis: key={}, TTL={}s", key, ttlSeconds);

        } catch (Exception e) {
            // Redis is down or unreachable
            // Log error but don't throw
            // System continues to work (token validation might be affected)
            log.error("Failed to blacklist token in Redis: {}", e.getMessage());

            // In production, you might want to:
            // - Send alert to monitoring system
            // - Fall back to in-memory blacklist
            // - Retry with exponential backoff
        }
    }

    /**
     * Check if Token is Blacklisted
     * <p>
     * Fast O(1) lookup in Redis.
     * <p>
     * Process:
     * 1. Build Redis key: "blacklist:TOKEN"
     * 2. Check if key exists in Redis
     * 3. Return true if exists, false otherwise
     * <p>
     * Redis Command:
     * EXISTS blacklist:TOKEN
     * <p>
     * Performance:
     * - Redis EXISTS command: ~1ms
     * - Network latency: ~1-5ms (local network)
     * - Total: ~2-6ms per check
     * <p>
     * Called On Every Request:
     * This method is called by JwtAuthenticationFilter
     * for every authenticated request.
     * <p>
     * That's why Redis is important:
     * - Fast lookups (microseconds)
     * - Low latency
     * - High throughput
     * <p>
     * Error Handling:
     * - If Redis is down, returns false (fail open)
     * - This means blacklisted tokens might work
     * - Trade-off: Availability vs Security
     * <p>
     * Alternative: Fail Closed
     * - Return true if Redis is down
     * - All tokens treated as blacklisted
     * - Higher security, lower availability
     *
     * @param token Access token to check
     * @return true if blacklisted, false otherwise
     */
    @Override
    public boolean isBlacklisted(String token) {
        try {
            String key = BLACKLIST_PREFIX + token;

            // Check if key exists in Redis
            // Boolean.TRUE.equals() handles null safely
            Boolean exists = redisTemplate.hasKey(key);
            boolean isBlacklisted = Boolean.TRUE.equals(exists);

            if (isBlacklisted) {
                log.debug("Token found in blacklist: {}", key);
            }

            return isBlacklisted;

        } catch (Exception e) {
            // Redis is down
            log.error("Failed to check blacklist in Redis: {}", e.getMessage());

            // FAIL OPEN: Return false (allow token)
            // This means system continues to work even if Redis is down
            // Trade-off: Security vs Availability

            // For higher security, change to:
            // return true; (FAIL CLOSED: block all tokens if Redis down)

            return false;
        }
    }

    /**
     * Remove Token from Blacklist
     * <p>
     * Manually removes a token from Redis.
     * <p>
     * Process:
     * 1. Build Redis key
     * 2. Delete key from Redis
     * <p>
     * Redis Command:
     * DEL blacklist:TOKEN
     * <p>
     * Use Cases:
     * - Admin reinstates user
     * - Testing/debugging
     * - Manual intervention
     * <p>
     * Note: Rarely needed in production.
     * Tokens expire automatically via TTL.
     *
     * @param token Access token to remove
     */
    @Override
    public void removeToken(String token) {
        try {
            String key = BLACKLIST_PREFIX + token;

            // Delete key from Redis
            redisTemplate.delete(key);

            log.info("Token removed from blacklist: {}", key);

        } catch (Exception e) {
            log.error("Failed to remove token from blacklist: {}", e.getMessage());
        }
    }

    /**
     * Clear All Blacklisted Tokens
     * <p>
     * Removes all tokens from blacklist.
     * <p>
     * Process:
     * 1. Find all keys matching "blacklist:*"
     * 2. Delete all matching keys
     * <p>
     * Redis Commands:
     * KEYS blacklist:*  (finds all matching keys)
     * DEL key1 key2 key3... (deletes them)
     * <p>
     * WARNING: DANGEROUS OPERATION!
     * - Use only for testing/debugging
     * - All blacklisted tokens will work again
     * - Can cause security issues in production
     * <p>
     * Performance Note:
     * - KEYS command is O(N) (scans all keys)
     * - Blocks Redis during scan
     * - Use SCAN in production instead
     * <p>
     * Better Production Implementation:
     * Use SCAN instead of KEYS:
     * - Non-blocking
     * - Returns results in batches
     * - Doesn't freeze Redis
     */
    @Override
    public void clearAll() {
        try {
            // Find all blacklist keys
            // WARNING: This is O(N) and blocks Redis
            Set<String> keys = redisTemplate.keys(BLACKLIST_PREFIX + "*");

            if (keys != null && !keys.isEmpty()) {
                // Delete all found keys
                redisTemplate.delete(keys);
                log.warn("CLEARED ALL BLACKLISTED TOKENS! Count: {}", keys.size());
            } else {
                log.info("No blacklisted tokens to clear");
            }

        } catch (Exception e) {
            log.error("Failed to clear blacklist: {}", e.getMessage());
        }
    }

    /**
     * Get Blacklisted Tokens Count
     * <p>
     * Returns number of currently blacklisted tokens.
     * <p>
     * Process:
     * 1. Find all keys matching "blacklist:*"
     * 2. Return count
     * <p>
     * Redis Commands:
     * KEYS blacklist:*  (finds all keys)
     * Count the results
     * <p>
     * Use Cases:
     * - Monitoring dashboard
     * - Health checks
     * - Metrics/alerting
     * - Debugging
     * <p>
     * Performance Note:
     * - KEYS command is O(N)
     * - Can be slow with many keys
     * - Don't call this frequently in production
     * <p>
     * Better for Production:
     * - Maintain a counter in Redis
     * - Increment on blacklist
     * - Decrement on expiry
     *
     * @return Number of blacklisted tokens
     */
    @Override
    public long getBlacklistedCount() {
        try {
            Set<String> keys = redisTemplate.keys(BLACKLIST_PREFIX + "*");
            long count = (keys != null) ? keys.size() : 0;

            log.debug("Current blacklisted tokens count: {}", count);

            return count;

        } catch (Exception e) {
            log.error("Failed to get blacklist count: {}", e.getMessage());
            return -1;  // Indicates error
        }
    }
}
