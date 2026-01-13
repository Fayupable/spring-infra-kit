package io.fayupable.jwtrefreshtoken.service.cookie.blacklist;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-Memory Token Blacklist Service
 * <p>
 * Fallback implementation when Redis is unavailable.
 * <p>
 * Conditional Loading:
 * Loads ONLY if RedisTokenBlacklistService is NOT available.
 * <p>
 * Spring Boot checks:
 * 1. Is RedisTokenBlacklistService bean created?
 * 2. If NO → Create this bean (fallback)
 * 3. If YES → Skip this (use Redis)
 * <p>
 * Storage:
 * - ConcurrentHashMap (thread-safe)
 * - Key: Token string
 * - Value: Expiry time (Instant)
 * <p>
 * Limitations:
 * - Not distributed (single server only)
 * - Lost on app restart
 * - Memory-based (can grow large)
 * - Manual cleanup needed
 * <p>
 * When to Use:
 * - Development/testing
 * - Small deployments
 * - Redis unavailable
 * <p>
 * Production Recommendation:
 * Always use Redis for production.
 * This is just a fallback.
 */
@Service("inMemoryTokenBlacklistService")
@Slf4j
@ConditionalOnMissingClass("org.springframework.data.redis.core.RedisTemplate")
public class InMemoryTokenBlacklistService implements TokenBlacklistService {

    /**
     * In-memory storage
     * <p>
     * Thread-safe map storing blacklisted tokens.
     * <p>
     * Key: Token string
     * Value: Expiry timestamp
     * <p>
     * ConcurrentHashMap because:
     * - Multiple threads access this (every request)
     * - Thread-safe operations
     * - Better performance than synchronized Map
     */
    private final Map<String, Instant> blacklistedTokens = new ConcurrentHashMap<>();

    /**
     * Constructor logs fallback warning
     */
    public InMemoryTokenBlacklistService() {
        log.warn("====================================================");
        log.warn("USING IN-MEMORY TOKEN BLACKLIST (Redis not available)");
        log.warn("Blacklist will be lost on application restart!");
        log.warn("For production, please configure Redis.");
        log.warn("====================================================");
    }

    /**
     * Add Token to Blacklist
     * <p>
     * Stores token in memory with expiry time.
     * <p>
     * Process:
     * 1. Calculate expiry: now + TTL
     * 2. Store in map: token → expiry
     * 3. Scheduled cleanup will remove expired
     *
     * @param token      Access token to blacklist
     * @param ttlSeconds Time to live in seconds
     */
    @Override
    public void blacklistToken(String token, long ttlSeconds) {
        Instant expiryTime = Instant.now().plusSeconds(ttlSeconds);
        blacklistedTokens.put(token, expiryTime);

        log.debug("Token blacklisted in memory: TTL={}s, Total={}",
                ttlSeconds, blacklistedTokens.size());
    }

    /**
     * Check if Token is Blacklisted
     * <p>
     * Fast O(1) lookup in HashMap.
     * <p>
     * Process:
     * 1. Get expiry time from map
     * 2. If not found → false
     * 3. If found but expired → remove and return false
     * 4. If found and valid → true
     * <p>
     * Auto-cleanup:
     * If token expired, removes it immediately.
     *
     * @param token Access token to check
     * @return true if blacklisted, false otherwise
     */
    @Override
    public boolean isBlacklisted(String token) {
        Instant expiryTime = blacklistedTokens.get(token);

        if (expiryTime == null) {
            return false;  // Not in blacklist
        }

        // Check if expired
        if (Instant.now().isAfter(expiryTime)) {
            // Expired, remove and return false
            blacklistedTokens.remove(token);
            return false;
        }

        // Valid and blacklisted
        return true;
    }

    /**
     * Remove Token from Blacklist
     * <p>
     * Simple map removal.
     *
     * @param token Access token to remove
     */
    @Override
    public void removeToken(String token) {
        blacklistedTokens.remove(token);
        log.debug("Token removed from memory blacklist");
    }

    /**
     * Clear All Blacklisted Tokens
     * <p>
     * Clears the entire map.
     * <p>
     * WARNING: Use with caution!
     */
    @Override
    public void clearAll() {
        int count = blacklistedTokens.size();
        blacklistedTokens.clear();
        log.warn("CLEARED ALL BLACKLISTED TOKENS from memory! Count: {}", count);
    }

    /**
     * Get Blacklisted Tokens Count
     * <p>
     * Returns map size.
     * <p>
     * Note: Includes expired tokens (until cleanup runs).
     *
     * @return Number of tokens in memory
     */
    @Override
    public long getBlacklistedCount() {
        return blacklistedTokens.size();
    }

    /**
     * Cleanup Expired Tokens
     * <p>
     * Scheduled task that removes expired tokens.
     * <p>
     * Why Needed?
     * - Redis has automatic expiry (TTL)
     * - In-memory doesn't
     * - Without cleanup, map grows forever
     * <p>
     * Schedule: Every 15 minutes
     * Cron: "0 0/15 * * * ?" = minute 0, 15, 30, 45
     * <p>
     * Process:
     * 1. Loop through all tokens
     * 2. Check if expired
     * 3. Remove expired ones
     * 4. Log cleanup stats
     * <p>
     * Performance:
     * - O(N) where N = number of tokens
     * - Runs in background thread
     * - Doesn't block requests
     */
    @Scheduled(cron = "0 0/15 * * * ?")  // Every 15 minutes
    public void cleanupExpiredTokens() {
        Instant now = Instant.now();
        int initialSize = blacklistedTokens.size();

        // Remove expired tokens
        blacklistedTokens.entrySet().removeIf(entry ->
                now.isAfter(entry.getValue())
        );

        int removedCount = initialSize - blacklistedTokens.size();

        if (removedCount > 0) {
            log.info("Cleaned up {} expired tokens from memory blacklist. Remaining: {}",
                    removedCount, blacklistedTokens.size());
        }
    }
}