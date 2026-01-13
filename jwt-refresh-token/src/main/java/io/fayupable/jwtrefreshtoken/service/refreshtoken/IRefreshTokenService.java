package io.fayupable.jwtrefreshtoken.service.refreshtoken;

import io.fayupable.jwtrefreshtoken.entity.RefreshToken;
import io.fayupable.jwtrefreshtoken.response.LogoutResponse;
import io.fayupable.jwtrefreshtoken.response.RefreshTokenResponse;
import org.springframework.http.ResponseCookie;

import java.util.UUID;

/**
 * Refresh Token Service Interface
 * <p>
 * Defines contract for refresh token management operations.
 * <p>
 * Core Responsibilities:
 * - Create refresh tokens after login
 * - Validate and rotate tokens during refresh
 * - Revoke tokens on logout
 * - Cleanup expired tokens
 * - Manage refresh token cookies (HttpOnly)
 * <p>
 * Benefits of Interface:
 * - Easy to mock in tests
 * - Can swap implementations (e.g., Redis-based)
 * - Clear contract for service consumers
 */
public interface IRefreshTokenService {

    /**
     * Create and store refresh token
     * <p>
     * Called after successful login.
     * Stores hashed token in database with device info.
     *
     * @param userId     User's unique identifier
     * @param tokenValue Plain refresh token (to be hashed)
     * @param deviceInfo User agent string
     * @param ipAddress  User's IP address
     */
    void createRefreshToken(UUID userId, String tokenValue, String deviceInfo, String ipAddress);

    /**
     * Validate token and rotate (create new one)
     * <p>
     * Called during token refresh request.
     * <p>
     * Flow:
     * 1. Validate token (exists, not expired, not revoked)
     * 2. Mark old token as revoked
     * 3. Create new token
     * 4. Return new token
     * <p>
     * This is token rotation - prevents replay attacks.
     *
     * @param tokenValue Plain refresh token from client
     * @return New refresh token entity
     */
    RefreshToken validateAndRotateToken(String tokenValue);

    /**
     * Revoke all tokens for a user
     * <p>
     * Called during:
     * - "Logout from all devices" action
     * - Password change (security measure)
     * - Account suspension
     *
     * @param userId User's unique identifier
     */
    void revokeAllUserTokens(UUID userId);

    /**
     * Revoke single token
     * <p>
     * Called during normal logout.
     *
     * @param tokenValue Plain refresh token to revoke
     */
    void revokeToken(String tokenValue);

    /**
     * Cleanup expired and old revoked tokens
     * <p>
     * Called by scheduled task.
     * Removes:
     * - Expired tokens (sliding window or absolute expiry)
     * - Revoked tokens older than 24 hours
     * <p>
     * Note: This is a simple implementation.
     * For production, consider Kafka-based async cleanup.
     */
    void cleanupExpiredTokens();

    /**
     * Validate token without rotation
     * <p>
     * Just checks if token is valid.
     * Does not create new token.
     *
     * @param tokenValue Plain refresh token
     * @throws RuntimeException if token invalid
     */
    void validateTokenOnly(String tokenValue);

    /**
     * Refresh access token
     * <p>
     * Main refresh operation.
     * <p>
     * Flow:
     * 1. Validate refresh token
     * 2. Rotate refresh token (old revoked, new created)
     * 3. Generate new access token
     * 4. Return both tokens (DTO only, no cookie)
     * <p>
     * Note: Controller should call createRefreshTokenCookie()
     * separately to get the cookie header.
     *
     * @param refreshToken Plain refresh token from client
     * @param userAgent    User agent for new token
     * @param ipAddress    IP address for new token
     * @return Response DTO with new access and refresh tokens
     */
    RefreshTokenResponse refreshToken(String refreshToken, String userAgent, String ipAddress);

    /**
     * Check if token is valid
     *
     * @param refreshToken Plain refresh token
     * @return true if valid, false otherwise
     */
    boolean isTokenValid(String refreshToken);

    /**
     * Logout user
     * <p>
     * Revokes refresh token and optionally blacklists access token.
     * <p>
     * Note: Controller should call clearRefreshTokenCookie()
     * separately to get the cookie clearing header.
     *
     * @param refreshToken Plain refresh token to revoke
     * @param accessToken  Access token to blacklist (optional)
     * @return Logout response DTO
     */
    LogoutResponse logout(String refreshToken, String accessToken);

    // ==================================================================================
    // COOKIE MANAGEMENT METHODS
    // These methods are called by Controller to manage HttpOnly cookies
    // ==================================================================================

    /**
     * Create Refresh Token Cookie
     * <p>
     * Creates HttpOnly cookie for secure token storage.
     * Called by Controller after successful token refresh or login.
     * <p>
     * Cookie Properties:
     * - name: "refresh_token"
     * - httpOnly: true (prevents JavaScript access - XSS protection)
     * - secure: from config (HTTPS only in production)
     * - sameSite: from config (CSRF protection)
     * - path: "/" (available for all routes)
     * - maxAge: 30 days (same as token expiry)
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
    ResponseCookie createRefreshTokenCookie(String refreshToken);

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
     * Important: Properties Must Match Original Cookie:
     * - name: "refresh_token"
     * - path: "/"
     * - domain: from config
     * <p>
     * Called During:
     * - User logout
     * - Token revocation
     * - Security breach detected
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
    ResponseCookie clearRefreshTokenCookie();
}