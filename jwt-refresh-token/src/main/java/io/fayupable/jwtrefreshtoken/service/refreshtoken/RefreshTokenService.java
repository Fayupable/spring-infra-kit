package io.fayupable.jwtrefreshtoken.service.refreshtoken;

import io.fayupable.jwtrefreshtoken.entity.RefreshToken;
import io.fayupable.jwtrefreshtoken.entity.User;
import io.fayupable.jwtrefreshtoken.repository.RefreshTokenRepository;
import io.fayupable.jwtrefreshtoken.repository.UserRepository;
import io.fayupable.jwtrefreshtoken.response.LogoutResponse;
import io.fayupable.jwtrefreshtoken.response.RefreshTokenResponse;
import io.fayupable.jwtrefreshtoken.security.jwt.JwtUtils;
import io.fayupable.jwtrefreshtoken.service.cookie.CookieService;
import io.fayupable.jwtrefreshtoken.service.cookie.blacklist.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseCookie;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Refresh Token Service Implementation
 * <p>
 * Manages refresh token lifecycle with cookie support:
 * - Creation after login (with HttpOnly cookie)
 * - Validation during refresh
 * - Rotation (security best practice)
 * - Revocation on logout (clear cookie)
 * - Cleanup of expired tokens
 * <p>
 * Security Features:
 * - Tokens are hashed (SHA-256) before storage
 * - Token rotation prevents replay attacks
 * - Sliding window + absolute expiry
 * - Device and IP tracking
 * - HttpOnly cookies (prevents XSS attacks)
 * <p>
 * Clean Architecture:
 * - Public methods: Interface implementation
 * - Private helpers: Single responsibility
 * - Detailed documentation for each method
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RefreshTokenService implements IRefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;
    private final CookieService cookieService;
    private final TokenBlacklistService tokenBlacklistService;


    @Value("${auth.token.cleanup.retentionHours:12}")
    private int retentionHours;

    @Value("${auth.token.cleanup.intervalMinutes:30}")
    private int intervalMinutes;


    /**
     * Create Refresh Token
     * <p>
     * Called after successful login.
     * Creates refresh token entity and stores in database.
     * <p>
     * Process:
     * 1. Hash the plain token with SHA-256 (security)
     * 2. Calculate expiry times (sliding + absolute)
     * 3. Build RefreshToken entity
     * 4. Save to database
     * <p>
     * Security Note:
     * - Token is hashed before storage (like password hashing)
     * - If database compromised, tokens can't be used
     * - SHA-256 is fast and deterministic (unlike BCrypt)
     *
     * @param userId     User's unique identifier
     * @param tokenValue Plain refresh token (NOT hashed yet)
     * @param deviceInfo User agent string from request
     * @param ipAddress  User's IP address from request
     */
    @Override
    @Transactional
    public void createRefreshToken(UUID userId, String tokenValue, String deviceInfo, String ipAddress) {
        log.debug("Creating refresh token for user: {}", userId);

        String tokenHash = hashToken(tokenValue);
        LocalDateTime now = LocalDateTime.now();

        RefreshToken refreshToken = buildRefreshTokenEntity(
                userId,
                tokenHash,
                now,
                calculateSlidingExpiry(now),
                calculateAbsoluteExpiry(now),
                deviceInfo,
                ipAddress
        );

        saveRefreshToken(refreshToken);

        log.info("Refresh token created for user: {} from IP: {}", userId, ipAddress);
    }

    /**
     * Validate and Rotate Token
     * <p>
     * Main refresh operation implementing token rotation pattern.
     * <p>
     * UPDATED: No longer creates token internally.
     * Just validates and returns old token entity.
     * Actual rotation happens in refreshToken() method.
     * <p>
     * Token Rotation Flow (NEW):
     * 1. Validate incoming token (exists, not expired, not revoked)
     * 2. Return validated token entity
     * 3. Caller (refreshToken method) generates NEW JWT
     * 4. Caller calls rotateToken() with NEW JWT
     * <p>
     * Why This Change?
     * - Separation of concerns (validation vs creation)
     * - refreshToken() handles JWT generation
     * - This method only validates
     * - Cleaner architecture
     * <p>
     * Why Token Rotation?
     * - Prevents replay attacks (stolen tokens become useless)
     * - Limits damage window if token compromised
     * - Industry security best practice
     * - Provides audit trail of token usage
     *
     * @param tokenValue Plain refresh token (JWT) from client
     * @return Validated RefreshToken entity (old token, not yet rotated)
     * @throws RuntimeException if token invalid, expired, or revoked
     */
    @Override
    @Transactional
    public RefreshToken validateAndRotateToken(String tokenValue) {
        log.debug("Validating refresh token");

        // Just validate and return - no rotation here anymore
        RefreshToken validatedToken = findAndValidateToken(tokenValue);

        log.info("Token validated for user: {}", validatedToken.getUserId());

        return validatedToken;
    }

    /**
     * Revoke All User Tokens
     * <p>
     * Bulk revocation operation for security scenarios.
     * <p>
     * Use Cases:
     * - "Logout from all devices" feature
     * - Password change (security: invalidate all sessions)
     * - Account suspension by admin
     * - Security breach detected
     * <p>
     * Implementation:
     * - Uses bulk UPDATE query for performance
     * - Marks all user's tokens as revoked
     * - Does not delete (keeps audit trail)
     *
     * @param userId User's unique identifier
     */
    @Override
    @Transactional
    public void revokeAllUserTokens(UUID userId) {
        log.info("Revoking all tokens for user: {}", userId);
        refreshTokenRepository.revokeAllUserTokens(userId);
    }

    /**
     * Revoke Single Token
     * <p>
     * Called during normal logout.
     * Marks specific token as revoked.
     * <p>
     * Process:
     * 1. Hash incoming token
     * 2. Find token in database
     * 3. Mark as revoked
     * 4. Save to database
     * <p>
     * Note: Token is not deleted, only marked as revoked.
     * This provides audit trail and prevents replay attacks.
     *
     * @param tokenValue Plain refresh token to revoke
     * @throws RuntimeException if token not found or already revoked
     */
    @Override
    @Transactional
    public void revokeToken(String tokenValue) {
        String tokenHash = hashToken(tokenValue);
        RefreshToken token = findValidTokenByHash(tokenHash);

        markTokenAsRevoked(token);

        log.info("Token revoked for user: {}", token.getUserId());
    }

    /**
     * Cleanup Expired Tokens
     * <p>
     * Scheduled task that removes old tokens from database.
     * <p>
     * Cleanup Schedule:
     * - Default: Every 30 minutes
     * - Configurable via cron expression
     * <p>
     * Cleanup Criteria:
     * - Expired tokens (sliding window OR absolute expiry passed)
     * - Revoked tokens older than configured retention period
     * <p>
     * Configuration:
     * auth:
     * token:
     * cleanup:
     * retentionHours: 12      # How long to keep revoked tokens
     * intervalMinutes: 30     # Cleanup frequency (for reference)
     * <p>
     * Why Keep Revoked Tokens for Retention Period?
     * - Detect replay attacks (reuse of revoked token)
     * - Provide audit trail for recent logouts
     * - Grace period for security investigation
     * - Help detect suspicious patterns
     * <p>
     * Performance:
     * - Uses batch processing (100 tokens per batch)
     * - Prevents memory issues with large datasets
     * - Uses indexed queries for efficiency
     * <p>
     * IMPORTANT PRODUCTION NOTE:
     * <p>
     * This is a SIMPLE implementation using @Scheduled tasks.
     * <p>
     * For PRODUCTION with HIGH TOKEN VOLUME, strongly recommend KAFKA:
     * <p>
     * Benefits of Kafka Approach:
     * <p>
     * 1. Decoupled Architecture:
     * - Cleanup runs in separate microservice
     * - Main service doesn't block for cleanup
     * - Better horizontal scalability
     * - Can deploy cleanup service independently
     * <p>
     * 2. Reliability & Fault Tolerance:
     * - Kafka ensures guaranteed message delivery
     * - Can retry failed cleanup operations
     * - Dead letter queue for problematic tokens
     * - No data loss if cleanup service crashes
     * <p>
     * 3. Performance & Efficiency:
     * - No load on main application database
     * - Can batch cleanup operations effectively
     * - Can schedule during off-peak hours
     * - Better resource utilization
     * <p>
     * 4. Monitoring & Observability:
     * - Track cleanup metrics via Kafka
     * - Real-time alerts on cleanup failures
     * - Complete audit trail of deletions
     * - Easy integration with monitoring tools
     * <p>
     * Kafka Implementation Example:
     * <p>
     * // In RefreshTokenService:
     * public void scheduleTokenCleanup(UUID tokenId) {
     * TokenCleanupEvent event = new TokenCleanupEvent(tokenId, LocalDateTime.now());
     * kafkaTemplate.send("token-cleanup-topic", event);
     * }
     * <p>
     * // In separate CleanupService:
     *
     * @Service public class TokenCleanupService {
     * @KafkaListener(topics = "token-cleanup-topic")
     * public void processCleanup(TokenCleanupEvent event) {
     * // Perform cleanup in background
     * // Can batch multiple events
     * // Can retry on failure
     * }
     * }
     * <p>
     * Since Kafka is not the main focus of this demo module,
     * we use simpler @Scheduled approach for clarity.
     * <p>
     * However, for real production systems with:
     * - High user traffic (1000+ logins/day)
     * - Multiple services/instances
     * - Strict performance requirements
     * <p>
     * STRONGLY RECOMMEND implementing Kafka-based async cleanup.
     * <p>
     * Cron Schedule: "0 0/30 * * * ?" = Every 30 minutes
     * <p>
     * Note: Spring @Scheduled doesn't support dynamic cron from @Value.
     * To change the interval, update the cron expression in the annotation.
     * <p>
     * Common Cron Expressions:
     * - Every 15 minutes: "0 0/15 * * * ?"
     * - Every 30 minutes: "0 0/30 * * * ?"
     * - Every hour: "0 0 * * * ?"
     * - Every 2 hours: "0 0 0/2 * * ?"
     */
    @Override
    @Transactional
    @Scheduled(cron = "0 0/30 * * * ?")  // Every 30 minutes
    public void cleanupExpiredTokens() {
        log.info("Starting scheduled cleanup of expired tokens (retention: {} hours, interval: {} minutes)",
                retentionHours, intervalMinutes);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = calculateCleanupCutoff(now);

        log.debug("Cleanup cutoff time: {} (tokens revoked before this will be deleted)", cutoff);

        int totalDeleted = performBatchCleanup(now, cutoff);

        log.info("Cleanup completed. Deleted {} expired/old tokens (retention: {} hours, interval: {} minutes)",
                totalDeleted, retentionHours, intervalMinutes);
    }

    /**
     * Validate Token Only
     * <p>
     * Simple validation without rotation.
     * Used when you need to check token validity without creating new token.
     * <p>
     * Checks:
     * - Token exists in database
     * - Token is not revoked
     * - Token is not expired
     * <p>
     * Does NOT:
     * - Create new token
     * - Update database
     * - Rotate token
     *
     * @param tokenValue Plain refresh token to validate
     * @throws RuntimeException if invalid, revoked, or expired
     */
    @Override
    @Transactional(readOnly = true)
    public void validateTokenOnly(String tokenValue) {
        findAndValidateToken(tokenValue);
    }

    /**
     * Refresh Access Token
     * <p>
     * Main refresh endpoint operation with full flow.
     * <p>
     * CORRECTED Complete Flow:
     * 1. Validate refresh token (check exists, not expired, not revoked)
     * 2. Load user from database (get latest data)
     * 3. Extract user's roles (for access token)
     * 4. Generate NEW access token (JWT, short-lived, 15 min)
     * 5. Generate NEW refresh token (JWT, long-lived, 30 days)
     * 6. Rotate token (save new JWT hash, revoke old) ← ÖNEMLİ!
     * 7. Build response with both JWT tokens
     * <p>
     * Token Rotation Security (CORRECTED):
     * - Old refresh token (JWT) becomes invalid
     * - New refresh token (JWT) has new signature & timestamp
     * - Both tokens are JWTs (NOT UUIDs)
     * - Database stores JWT hashes (NOT UUID hashes)
     * - Client receives JWTs (NOT UUIDs)
     * <p>
     * Why Generate JWT Before Rotation?
     * - rotateToken() needs JWT to hash and store
     * - JWT contains user info (email, userId)
     * - JWT has signature and expiration
     * - UUID doesn't have any of these
     * <p>
     * Flow Diagram:
     * Client → [Old JWT] → Validate → Load User → Generate New JWTs → Rotate → Client
     * ↓
     * [New Access JWT]
     * [New Refresh JWT]
     * ↓
     * rotateToken() creates new entity
     * Hash & Store in DB
     * Revoke old token
     * <p>
     * Security Benefits:
     * - Every refresh creates NEW JWT tokens
     * - Old JWT immediately revoked (can't reuse)
     * - Stolen old JWT is useless after rotation
     * - Audit trail via token linking
     *
     * @param refreshToken Plain refresh token (JWT) from client
     * @param userAgent    User agent string for tracking
     * @param ipAddress    IP address for tracking/security
     * @return Response with new access JWT and refresh JWT
     * @throws RuntimeException if token invalid or user not found
     */
    @Override
    @Transactional
    public RefreshTokenResponse refreshToken(String refreshToken, String userAgent, String ipAddress) {
        log.info("Token refresh request from IP: {}", ipAddress);

        // Step 1: Validate token (does NOT rotate yet, just validates)
        RefreshToken oldTokenEntity = validateAndRotateToken(refreshToken);

        // Step 2: Load user and extract roles
        User user = loadUser(oldTokenEntity.getUserId());
        List<String> roles = extractUserRoles(user);

        // Step 3: Generate NEW access token (JWT)
        String newAccessToken = jwtUtils.generateAccessToken(
                user.getEmail(),
                user.getUserId().toString(),
                roles
        );

        // Step 4: Generate NEW refresh token
        String newRefreshToken = jwtUtils.generateRefreshToken(
                user.getEmail(),
                user.getUserId().toString()
        );

        // Step 5: Rotate token (pass JWT, creates new entity, revokes old)
        rotateToken(oldTokenEntity, newRefreshToken);

        log.info("Token refresh successful for user: {}. Old JWT revoked, new JWT issued",
                user.getEmail());

        // Step 6: Build response with BOTH JWT tokens
        return buildRefreshTokenResponse(newAccessToken, newRefreshToken);
    }

    /**
     * Check if Token is Valid
     * <p>
     * Convenience method for boolean check.
     * Wraps validateTokenOnly() with try-catch.
     * <p>
     * Use When:
     * - Need simple true/false answer
     * - Don't want exception handling
     * - Checking token before using it
     *
     * @param refreshToken Plain refresh token to check
     * @return true if valid (exists, not revoked, not expired), false otherwise
     */
    @Override
    @Transactional(readOnly = true)
    public boolean isTokenValid(String refreshToken) {
        try {
            validateTokenOnly(refreshToken);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Logout User
     * <p>
     * Complete logout operation with token cleanup.
     * <p>
     * Logout Flow:
     * 1. Revoke refresh token (mark as revoked in database)
     * 2. Blacklist access token (prevent immediate reuse)
     * 3. Build logout response (signals client to clear tokens)
     * <p>
     * Why Blacklist Access Token?
     * - Access tokens are stateless (JWT)
     * - They don't check database on every request
     * - Blacklisting prevents reuse until expiry
     * - Uses Redis/In-Memory for fast lookup
     * <p>
     * Client Responsibilities:
     * - Clear refresh token cookie
     * - Clear access token from localStorage/memory
     * - Redirect to login page
     *
     * @param refreshToken Refresh token to revoke (can be null)
     * @param accessToken  Access token to blacklist (can be null)
     * @return Logout response with success status
     */
    @Override
    @Transactional
    public LogoutResponse logout(String refreshToken, String accessToken) {
        log.info("Logout request");

        // Step 1: Revoke refresh token (database)
        revokeRefreshTokenSafely(refreshToken);

        // Step 2: Blacklist access token (Redis/Memory)
        blacklistAccessTokenSafely(accessToken);

        // Step 3: Build response
        return buildLogoutResponse();
    }

    // ==================================================================================
    // These methods handle token validation logic with detailed error handling
    // ==================================================================================

    /**
     * Find token by hash and perform full validation
     * <p>
     * This is the main validation helper that combines multiple checks.
     * <p>
     * Validation Steps:
     * 1. Hash the plain token value
     * 2. Find token in database by hash
     * 3. Check if token is revoked
     * 4. Check if token is expired
     * <p>
     * Why Separate Method?
     * - Reused by multiple public methods
     * - Centralizes validation logic
     * - Consistent error handling
     * - Easy to modify validation rules
     *
     * @param tokenValue Plain token value from client
     * @return Valid RefreshToken entity
     * @throws RuntimeException if token not found, revoked, or expired
     */
    private RefreshToken findAndValidateToken(String tokenValue) {
        String tokenHash = hashToken(tokenValue);
        RefreshToken token = findValidTokenByHash(tokenHash);
        validateTokenNotExpired(token);
        return token;
    }

    /**
     * Find valid (non-revoked) token by hash
     * <p>
     * Queries database for token with:
     * - Matching hash value
     * - revoked = false
     * <p>
     * Why Check Revoked in Query?
     * - More efficient (database filter)
     * - Uses index for faster lookup
     * - Prevents loading revoked tokens
     *
     * @param tokenHash SHA-256 hash of token
     * @return RefreshToken entity
     * @throws RuntimeException if not found or already revoked
     */
    private RefreshToken findValidTokenByHash(String tokenHash) {
        return refreshTokenRepository.findByTokenHashAndRevokedFalse(tokenHash)
                .orElseThrow(() -> {
                    log.error("Invalid or revoked refresh token");
                    return new RuntimeException("Invalid or revoked refresh token");
                });
    }

    /**
     * Validate token is not expired
     * <p>
     * Checks both expiry conditions:
     * - Sliding window expiry (expiresAt)
     * - Absolute max expiry (maxExpiry)
     * <p>
     * Token is expired if EITHER condition is met.
     * <p>
     * Why Two Expiry Times?
     * - Sliding: Extends with each use (user convenience)
     * - Absolute: Hard limit regardless of use (security)
     * <p>
     * Example:
     * - Created: Jan 1
     * - expiresAt: Jan 31 (30 days, can extend)
     * - maxExpiry: Apr 1 (90 days, never extends)
     *
     * @param token RefreshToken to check
     * @throws RuntimeException if expired
     */
    private void validateTokenNotExpired(RefreshToken token) {
        if (token.isExpired()) {
            log.error("Refresh token expired for user: {}", token.getUserId());
            throw new RuntimeException("Refresh token expired. Please login again.");
        }
    }

    // ==================================================================================
    // These methods handle the token rotation process for security
    // ==================================================================================

    /**
     * Rotate Token with New JWT
     * <p>
     * Creates new token entity and revokes old one.
     * This is the CORRECT token rotation implementation.
     * <p>
     * Token Rotation Flow:
     * 1. Take the NEW JWT token as parameter (NOT UUID!)
     * 2. Hash the JWT token (SHA-256)
     * 3. Create new RefreshToken entity with JWT hash
     * 4. Save new token to database
     * 5. Revoke old token and link to new one
     * <p>
     * Why JWT as Parameter?
     * - Refresh token MUST be JWT (not UUID)
     * - JWT contains user info (email, userId)
     * - JWT is what client receives and sends back
     * - Database stores JWT hash, not UUID hash
     * <p>
     * Security Benefits:
     * - Old JWT becomes invalid (revoked)
     * - New JWT has different signature (timestamp changed)
     * - Even if old JWT stolen, it's useless
     * - Audit trail via token linking (replacedBy)
     * <p>
     * Process:
     * 1. Hash new JWT token (plain → hash)
     * 2. Build new RefreshToken entity
     * - userId: same user
     * - tokenHash: NEW JWT hash
     * - issuedAt: current time (NOT old token's time!)
     * - expiresAt: sliding window reset (30 days from now)
     * - maxExpiry: SAME as old token (absolute limit doesn't change)
     * - deviceInfo: same device
     * - ipAddress: same IP
     * 3. Save new token to database
     * 4. Revoke old token (set revoked = true)
     * 5. Link old → new (set replacedBy = new token's ID)
     * <p>
     * Why Reset issuedAt?
     * - This is a NEW token, not an update
     * - issuedAt represents when token was created
     * - JWT's "iat" claim also shows new timestamp
     * <p>
     * Why Reset expiresAt?
     * - Sliding window resets on each use
     * - Active users stay logged in (convenience)
     * - Inactive tokens expire (security)
     * <p>
     * Why Keep Same maxExpiry?
     * - Absolute limit never changes (security)
     * - Even with daily use, token dies after 90 days
     * - Prevents infinite token lifetime
     * - Compliance requirement (PCI-DSS, etc.)
     *
     * @param oldToken    Existing token being replaced
     * @param newJwtToken NEW JWT token value (plain, not hashed)
     */
    private void rotateToken(RefreshToken oldToken, String newJwtToken) {
        // Step 1: Hash the NEW JWT token
        String newTokenHash = hashToken(newJwtToken);
        LocalDateTime now = LocalDateTime.now();

        // Step 2: Build new token entity with JWT hash
        RefreshToken newToken = buildRefreshTokenEntity(
                oldToken.getUserId(),
                newTokenHash,                        // ← JWT hash (NOT UUID!)
                now,                                 // ← Current time (NEW token!)
                calculateSlidingExpiry(now),         // ← Reset sliding window
                oldToken.getMaxExpiry(),             // ← Keep absolute limit
                oldToken.getDeviceInfo(),
                oldToken.getIpAddress()
        );

        // Step 3: Save new token to database
        RefreshToken savedToken = saveRefreshToken(newToken);

        // Step 4: Revoke old token and create audit trail
        revokeAndLinkTokens(oldToken, savedToken);

        log.info("Token rotated for user: {}. Old token revoked, new JWT hash saved",
                oldToken.getUserId());
    }


    /**
     * Revoke old token and link to new token
     * <p>
     * Creates audit trail of token rotation.
     * <p>
     * This Linking Enables:
     * - Track token rotation history
     * - Detect replay attacks (old token reused)
     * - Revoke entire token family if compromise detected
     * - Audit user's session history
     * <p>
     * Token Family Example:
     * Token A (replaced) → Token B (replaced) → Token C (active)
     * <p>
     * If Token A is reused:
     * - Security breach detected!
     * - Can revoke entire family (A, B, C)
     * - Force user to re-login
     *
     * @param oldToken Token to revoke
     * @param newToken Replacement token
     */
    private void revokeAndLinkTokens(RefreshToken oldToken, RefreshToken newToken) {
        oldToken.setRevoked(true);
        oldToken.setReplacedBy(newToken.getRefreshTokenId());
        refreshTokenRepository.save(oldToken);
    }

    /**
     * Mark single token as revoked
     * <p>
     * Simple revocation without rotation.
     * Used during normal logout.
     * <p>
     * Sets:
     * - revoked = true
     * - updatedAt = current timestamp (via @PreUpdate)
     * <p>
     * Does NOT:
     * - Create new token
     * - Link to replacement
     *
     * @param token Token to revoke
     */
    private void markTokenAsRevoked(RefreshToken token) {
        token.setRevoked(true);
        refreshTokenRepository.save(token);
    }

    // ==================================================================================
    // These methods build and save refresh token entities
    // ==================================================================================

    /**
     * Build RefreshToken entity with all fields
     * <p>
     * Centralizes entity construction for consistency.
     * <p>
     * This Builder Pattern:
     * - Ensures all required fields set
     * - Makes code more readable
     * - Easy to modify in one place
     * - Type-safe construction
     * <p>
     * Field Descriptions:
     * - userId: Links token to user
     * - tokenHash: SHA-256 hash of token value
     * - issuedAt: When token was created
     * - expiresAt: Sliding window expiry (extends on use)
     * - maxExpiry: Absolute expiry (never extends)
     * - revoked: Token validity flag
     * - deviceInfo: User agent for tracking
     * - ipAddress: IP for security monitoring
     *
     * @param userId     User's unique identifier
     * @param tokenHash  SHA-256 hash of token
     * @param issuedAt   Creation timestamp
     * @param expiresAt  Sliding window expiry
     * @param maxExpiry  Absolute expiry limit
     * @param deviceInfo User agent string
     * @param ipAddress  IP address
     * @return RefreshToken entity (not yet saved)
     */
    private RefreshToken buildRefreshTokenEntity(UUID userId,
                                                 String tokenHash,
                                                 LocalDateTime issuedAt,
                                                 LocalDateTime expiresAt,
                                                 LocalDateTime maxExpiry,
                                                 String deviceInfo,
                                                 String ipAddress) {
        return RefreshToken.builder()
                .userId(userId)
                .tokenHash(tokenHash)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .maxExpiry(maxExpiry)
                .revoked(false)
                .deviceInfo(deviceInfo)
                .ipAddress(ipAddress)
                .build();
    }

    /**
     * Save refresh token to database
     * <p>
     * Wrapper method for repository save operation.
     * <p>
     * Why Separate Method?
     * - Centralizes database operations
     * - Can add logging/monitoring here
     * - Can add validation before save
     * - Easier to mock in tests
     * <p>
     * JPA Auto-Features:
     * - @PrePersist sets createdAt, updatedAt
     * - Generates UUID for refreshTokenId
     * - Validates constraints
     *
     * @param refreshToken Token entity to save
     * @return Saved entity with generated ID
     */
    private RefreshToken saveRefreshToken(RefreshToken refreshToken) {
        return refreshTokenRepository.save(refreshToken);
    }

    // ==================================================================================
    // These methods calculate various expiry timestamps
    // ==================================================================================

    /**
     * Calculate sliding window expiry
     * <p>
     * Sliding window resets each time token is used.
     * <p>
     * How Sliding Window Works:
     * - Token created: Jan 1, expires Jan 31 (30 days)
     * - Token used: Jan 15, expires Feb 14 (30 days from use)
     * - Token used: Feb 10, expires Mar 12 (30 days from use)
     * <p>
     * Benefits:
     * - Active users stay logged in
     * - Inactive tokens expire
     * - Better user experience
     * <p>
     * Constraint:
     * - Cannot extend beyond maxExpiry (absolute limit)
     *
     * @param from Base timestamp to calculate from
     * @return Expiry timestamp (30 days from base)
     */
    private LocalDateTime calculateSlidingExpiry(LocalDateTime from) {
        long expirationSeconds = jwtUtils.getRefreshTokenExpiration() / 1000;
        return from.plusSeconds(expirationSeconds);
    }

    /**
     * Calculate absolute max expiry
     * <p>
     * Absolute limit that never changes or extends.
     * <p>
     * Purpose:
     * - Force periodic re-authentication (security)
     * - Comply with security policies (e.g., PCI-DSS)
     * - Prevent infinite token lifetime
     * - Credential rotation best practice
     * <p>
     * Example:
     * - Token created: Jan 1
     * - maxExpiry: Apr 1 (90 days)
     * - Even with daily use, token dies on Apr 1
     * <p>
     * Typical Values:
     * - Sliding: 30 days (2592000000ms)
     * - Absolute: 90 days (7776000000ms)
     *
     * @param from Base timestamp (usually token creation time)
     * @return Absolute expiry timestamp (90 days from base)
     */
    private LocalDateTime calculateAbsoluteExpiry(LocalDateTime from) {
        long maxLifetimeSeconds = jwtUtils.getMaxAbsoluteLifetime() / 1000;
        return from.plusSeconds(maxLifetimeSeconds);
    }

    /**
     * Calculate cleanup cutoff time for revoked tokens
     * <p>
     * Determines how long to keep revoked tokens before deletion.
     * <p>
     * Current Policy: 24 hours
     * <p>
     * Why Keep Revoked Tokens?
     * - Detect replay attacks (reuse attempt)
     * - Audit trail (who logged out when)
     * - Security investigation (suspicious patterns)
     * - Grace period for sync issues
     * <p>
     * Why Delete After 24 Hours?
     * - Database cleanup (prevent bloat)
     * - Privacy (remove old data)
     * - Performance (smaller tables)
     * - Compliance (data retention policies)
     * <p>
     * Alternative Policies:
     * - 1 hour: Very aggressive cleanup
     * - 7 days: Extended audit trail
     * - 30 days: Compliance requirements
     *
     * @param now Current timestamp
     * @return Cutoff timestamp (12 hours ago)
     */
    private LocalDateTime calculateCleanupCutoff(LocalDateTime now) {
        return now.minusHours(retentionHours);
    }

    // ==================================================================================
    // These methods handle background cleanup of expired tokens
    // ==================================================================================

    /**
     * Perform batch cleanup of expired tokens
     * <p>
     * Deletes tokens in batches to prevent memory issues.
     * <p>
     * Batch Processing Benefits:
     * - Prevents loading all tokens into memory
     * - Allows progress logging
     * - Can stop/resume if needed
     * - Database-friendly (no huge queries)
     * <p>
     * Deletion Criteria:
     * - expiresAt < now (sliding window expired)
     * - maxExpiry < now (absolute limit expired)
     * - revoked = true AND updatedAt < cutoff (old revoked tokens)
     * <p>
     * Process:
     * 1. Query for batch of token IDs (not full entities)
     * 2. Delete by IDs
     * 3. Log progress
     * 4. Repeat until no more tokens
     * <p>
     * Error Handling:
     * - Catches exceptions (don't crash app)
     * - Logs errors for monitoring
     * - Continues with next scheduled run
     * <p>
     * Production Note:
     * For high-volume systems, use Kafka for async cleanup.
     * See detailed explanation in cleanupExpiredTokens() method.
     *
     * @param now    Current timestamp
     * @param cutoff Cutoff for old revoked tokens
     * @return Total number of tokens deleted
     */
    private int performBatchCleanup(LocalDateTime now, LocalDateTime cutoff) {
        int batchSize = 100;
        int totalDeleted = 0;

        try {
            List<UUID> expiredIds;
            do {
                // Find batch of expired token IDs
                expiredIds = findExpiredTokenIds(now, cutoff, batchSize);

                if (!expiredIds.isEmpty()) {
                    // Delete batch
                    int deleted = deleteTokensByIds(expiredIds);
                    totalDeleted += deleted;
                    log.debug("Deleted batch of {} tokens", deleted);
                }

                // Continue if more tokens exist
            } while (expiredIds.size() == batchSize);

        } catch (Exception e) {
            log.error("Error during token cleanup: {}", e.getMessage(), e);
            // In production with Kafka:
            // - Failed cleanup goes to dead letter queue
            // - Alert sent to monitoring system
            // - Can retry with exponential backoff
        }

        return totalDeleted;
    }

    /**
     * Find expired token IDs in batches
     * <p>
     * Queries only token IDs (not full entities) for efficiency.
     * <p>
     * Query Optimization:
     * - SELECT only refreshTokenId (not all columns)
     * - Uses indexes on expiresAt, maxExpiry, updatedAt
     * - Limit results to batch size
     * - Database does filtering (not application)
     * <p>
     * Why Return IDs Only?
     * - Less memory usage
     * - Faster query execution
     * - We only need IDs for deletion
     *
     * @param now       Current timestamp
     * @param cutoff    Cutoff for revoked tokens
     * @param batchSize Maximum tokens to return
     * @return List of token IDs to delete
     */
    private List<UUID> findExpiredTokenIds(LocalDateTime now, LocalDateTime cutoff, int batchSize) {
        return refreshTokenRepository.findExpiredOrRevokedTokenIds(
                now,
                cutoff,
                PageRequest.of(0, batchSize)
        );
    }

    /**
     * Delete tokens by their IDs
     * <p>
     * Bulk delete operation for efficiency.
     * <p>
     * SQL Generated:
     * DELETE FROM refresh_tokens WHERE refresh_token_id IN (...)
     * <p>
     * Performance:
     * - Single query (not N queries)
     * - Uses primary key index
     * - Fast execution
     *
     * @param ids List of token IDs to delete
     * @return Number of tokens actually deleted
     */
    private int deleteTokensByIds(List<UUID> ids) {
        return refreshTokenRepository.deleteByIds(ids);
    }

    // ==================================================================================
    // These methods handle user-related database operations
    // ==================================================================================

    /**
     * Load user from database by ID
     * <p>
     * Fetches user with EAGER-loaded roles.
     * <p>
     * Why EAGER Roles?
     * - Needed for access token generation
     * - Small dataset (1-3 roles per user)
     * - Avoids LazyInitializationException
     * - Single query more efficient
     * <p>
     * Error Handling:
     * - Throws RuntimeException if user not found
     * - Should never happen (token has valid userId)
     * - Indicates data consistency issue if occurs
     *
     * @param userId User's unique identifier
     * @return User entity with roles loaded
     * @throws RuntimeException if user not found
     */
    private User loadUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    /**
     * Extract role names from user entity
     * <p>
     * Converts Role entities to role name strings.
     * <p>
     * Transformation:
     * Set<Role> → List<String>
     * <p>
     * Example:
     * Input:  [Role(ROLE_USER), Role(ROLE_ADMIN)]
     * Output: ["ROLE_USER", "ROLE_ADMIN"]
     * <p>
     * Why Strings?
     * - JWT stores strings (not objects)
     * - Simpler serialization
     * - Spring Security expects strings
     *
     * @param user User entity with loaded roles
     * @return List of role name strings
     */
    private List<String> extractUserRoles(User user) {
        return user.getRoles().stream()
                .map(role -> role.getRoleName().name())
                .toList();
    }

    // ==================================================================================
    // These methods generate new token values and JWTs
    // ==================================================================================


    /**
     * Generate new access token for user
     * <p>
     * Creates JWT access token with user information.
     * <p>
     * Token Contents:
     * - Subject: user email
     * - Claim "id": user UUID
     * - Claim "roles": user roles array
     * - Issued at: current timestamp
     * - Expiration: 15 minutes from now
     * <p>
     * Token Purpose:
     * - Authenticate API requests
     * - Authorize access to resources
     * - Carry user identity and permissions
     * <p>
     * Security:
     * - Signed with HMAC-SHA256
     * - Cannot be modified without detection
     * - Short-lived (15 min reduces risk)
     *
     * @param user  User entity
     * @param roles User's role names
     * @return JWT access token string
     */
    private String generateNewAccessToken(User user, List<String> roles) {
        return jwtUtils.generateAccessToken(
                user.getEmail(),
                user.getUserId().toString(),
                roles
        );
    }


    // ==================================================================================
    // These methods construct response DTOs
    // ==================================================================================

    /**
     * Build refresh token response DTO with cookie
     * <p>
     * Constructs response with new tokens AND cookie header.
     * <p>
     * Response Contains:
     * - accessToken: New JWT (15 min)
     * - refreshToken: New refresh token (30 days)
     * - type: "Bearer" (OAuth 2.0 standard)
     * - expiresIn: Access token lifetime (milliseconds)
     * - success: true
     * - message: Success message
     * - cookieHeader: Set-Cookie header for HttpOnly cookie
     * <p>
     * Cookie Creation:
     * - Uses CookieService to create secure cookie
     * - HttpOnly (prevents XSS)
     * - Secure (HTTPS only in production)
     * - SameSite (CSRF protection)
     * <p>
     * Client Should:
     * - Store accessToken in memory/localStorage
     * - Browser automatically stores refresh token cookie
     * - Use accessToken in Authorization header
     * - Browser auto-sends cookie for /refresh endpoint
     *
     * @param accessToken  New JWT access token
     * @param refreshToken New refresh token value
     * @return Response DTO with cookie header
     */
    private RefreshTokenResponse buildRefreshTokenResponse(String accessToken, String refreshToken) {
        return RefreshTokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .type("Bearer")
                .expiresIn(jwtUtils.getRefreshTokenExpiration())
                .success(true)
                .message("Token refreshed successfully")
                .build();
    }

    /**
     * Build logout response DTO with cookie clearing
     * <p>
     * Simple success response for logout with cookie deletion.
     * <p>
     * Response Tells Client To:
     * - Clear refresh token cookie (via Set-Cookie header)
     * - Clear access token from storage
     * - Redirect to login page
     * - Show logout success message
     * <p>
     * Cookie Clearing:
     * - Uses CookieService to create deletion cookie
     * - Same name, path, domain as original cookie
     * - maxAge=0 (browser deletes immediately)
     * <p>
     * Fields:
     * - success: Always true (we reached here)
     * - message: User-friendly message
     * - shouldClearTokens: Signal to clear cookies
     * - cookieHeader: Set-Cookie header for cookie deletion
     *
     * @return Logout response DTO with cookie clearing header
     */
    private LogoutResponse buildLogoutResponse() {
        return LogoutResponse.builder()
                .success(true)
                .message("Logged out successfully")
                .shouldClearTokens(true)
                .build();
    }

    // ==================================================================================
    // These methods handle safe token revocation during logout
    // ==================================================================================

    /**
     * Revoke refresh token safely (no exception)
     * <p>
     * Attempts to revoke token, logs warning if fails.
     * <p>
     * Safe Means:
     * - Catches all exceptions
     * - Logs warning (not error)
     * - Doesn't throw
     * - Continues logout process
     * <p>
     * Why Safe?
     * - Logout should always succeed
     * - Don't block user from logging out
     * - Token might already be revoked
     * - Token might be invalid
     * <p>
     * Best Effort Approach:
     * - Try to revoke
     * - If fails, just warn
     * - User still logs out
     *
     * @param refreshToken Token to revoke (can be null)
     */
    private void revokeRefreshTokenSafely(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            try {
                revokeToken(refreshToken);
            } catch (Exception e) {
                log.warn("Failed to revoke refresh token: {}", e.getMessage());
                // Don't throw - logout should succeed anyway
            }
        }
    }

    /**
     * Blacklist access token safely (no exception)
     * <p>
     * Attempts to blacklist token in Redis/Memory.
     * <p>
     * Why Blacklist Access Token?
     * - Access tokens are stateless (JWT)
     * - They don't check database
     * - Valid for 15 minutes after issue
     * - Blacklisting prevents immediate reuse
     * <p>
     * Blacklist Storage:
     * - Redis (if available)
     * - In-Memory (fallback)
     * - TTL: 15 minutes (token lifetime)
     * <p>
     * Safe Operation:
     * - Catches exceptions
     * - Logs warning
     * - Doesn't block logout
     * <p>
     * Edge Cases Handled:
     * - Redis unavailable → uses in-memory
     * - Token already blacklisted → no error
     * - Invalid token format → just warn
     *
     * @param accessToken Token to blacklist (can be null)
     */
    private void blacklistAccessTokenSafely(String accessToken) {
        if (accessToken != null && !accessToken.isBlank()) {
            try {
                tokenBlacklistService.blacklistToken(accessToken, 900);  // 15 min TTL
            } catch (Exception e) {
                log.warn("Failed to blacklist access token: {}", e.getMessage());
                // Don't throw - logout should succeed anyway
            }
        }
    }

    // ==================================================================================
    // These methods handle SHA-256 hashing of tokens
    // ==================================================================================

    /**
     * Hash token with SHA-256
     * <p>
     * One-way hash function for secure token storage.
     * <p>
     * Why Hash Tokens?
     * - Database breach protection
     * - Similar to password hashing
     * - Can't reverse hash to get token
     * - Can only verify by hashing and comparing
     * <p>
     * Why SHA-256?
     * - Fast (unlike BCrypt)
     * - Deterministic (same input = same hash)
     * - One-way (can't reverse)
     * - Produces 64-character hex string
     * - Industry standard
     * <p>
     * Why Not BCrypt?
     * - BCrypt is for passwords (slow by design)
     * - Tokens need fast hashing (verified often)
     * - SHA-256 is sufficient for tokens
     * <p>
     * Security:
     * - Token is random UUID (high entropy)
     * - SHA-256 is cryptographically secure
     * - Database breach won't reveal tokens
     * <p>
     * Process:
     * 1. Convert token string to bytes (UTF-8)
     * 2. Hash bytes with SHA-256
     * 3. Convert hash bytes to hex string
     *
     * @param token Plain token string
     * @return 64-character hex string (SHA-256 hash)
     * @throws RuntimeException if SHA-256 unavailable (shouldn't happen)
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Convert byte array to hexadecimal string
     * <p>
     * Converts hash bytes to readable hex format.
     * <p>
     * Example:
     * Input:  [0x12, 0x34, 0xAB, 0xCD]
     * Output: "1234abcd"
     * <p>
     * Why Hex?
     * - Human-readable
     * - Fixed length (64 chars for SHA-256)
     * - URL-safe
     * - Easy to store in database
     * <p>
     * Process:
     * 1. For each byte (0-255)
     * 2. Convert to hex (00-ff)
     * 3. Ensure 2 digits (pad with 0 if needed)
     * 4. Append to string
     * <p>
     * Example Byte Conversion:
     * - 15 (0x0F) → "0f" (padded)
     * - 171 (0xAB) → "ab" (no padding needed)
     *
     * @param bytes Byte array to convert
     * @return Hexadecimal string representation
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');  // Pad single digit
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
    // ==================================================================================
    // PUBLIC METHODS - Cookie Management
    // These methods are called by Controller to manage cookies
    // ==================================================================================

    /**
     * Create Refresh Token Cookie
     * <p>
     * Creates HttpOnly cookie for secure token storage.
     * Called by Controller after successful token refresh.
     * <p>
     * Cookie Properties:
     * - name: "refresh_token"
     * - httpOnly: true (prevents JavaScript access - XSS protection)
     * - secure: from config (HTTPS only in production)
     * - sameSite: from config (CSRF protection)
     * - path: "/" (available for all routes)
     * - maxAge: 30 days (same as token expiry)
     * <p>
     * Why HttpOnly Cookie?
     * - Prevents XSS attacks (JavaScript can't read)
     * - Browser automatically sends with requests
     * - More secure than localStorage
     * - Industry best practice
     * <p>
     * Security Benefits:
     * - XSS can't steal token
     * - CSRF protection via SameSite
     * - Secure flag prevents HTTP transmission (production)
     * - Automatic expiry (browser enforced)
     * <p>
     * Usage in Controller:
     * <pre>
     * RefreshTokenResponse response = service.refreshToken(...);
     * ResponseCookie cookie = service.createRefreshTokenCookie(response.refreshToken());
     * return ResponseEntity.ok()
     *     .header(HttpHeaders.SET_COOKIE, cookie.toString())
     *     .body(response);
     * </pre>
     *
     * @param refreshToken Refresh token value to store in cookie
     * @return ResponseCookie for Set-Cookie header
     */
    public ResponseCookie createRefreshTokenCookie(String refreshToken) {
        log.debug("Creating refresh token cookie");

        // Calculate cookie max age (convert milliseconds to seconds)
        long cookieMaxAge = jwtUtils.getRefreshTokenExpiration() / 1000;  // 30 days in seconds

        // Use CookieService to create cookie with security settings
        ResponseCookie cookie = cookieService.createRefreshTokenCookie(refreshToken, cookieMaxAge);

        log.debug("Refresh token cookie created with maxAge: {} seconds", cookieMaxAge);

        return cookie;
    }

    /**
     * Clear Refresh Token Cookie
     * <p>
     * Creates cookie deletion directive.
     * Called by Controller after successful logout.
     * <p>
     * How Cookie Deletion Works:
     * - Can't directly "delete" a cookie
     * - Send cookie with same name but maxAge=0
     * - Browser interprets as "expire immediately"
     * - Browser removes expired cookie
     * <p>
     * Important: Properties Must Match:
     * - name: Must match original ("refresh_token")
     * - path: Must match original ("/")
     * - domain: Must match original (from config)
     * - If properties don't match, browser treats as different cookie
     * <p>
     * Called During:
     * - User logout
     * - Token revocation
     * - Security breach detected
     * - Session invalidation
     * <p>
     * Usage in Controller:
     * <pre>
     * LogoutResponse response = service.logout(...);
     * ResponseCookie clearCookie = service.clearRefreshTokenCookie();
     * return ResponseEntity.ok()
     *     .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
     *     .body(response);
     * </pre>
     *
     * @return ResponseCookie that clears the refresh token cookie
     */
    public ResponseCookie clearRefreshTokenCookie() {
        log.debug("Creating cookie deletion directive");

        // Use CookieService to create deletion cookie
        ResponseCookie clearCookie = cookieService.clearRefreshTokenCookie();

        log.debug("Cookie deletion directive created");

        return clearCookie;
    }

}