package io.fayupable.jwtrefreshtoken.service.cookie;

import io.fayupable.jwtrefreshtoken.config.CookieConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Cookie Service
 * <p>
 * Centralized cookie management for refresh tokens.
 * <p>
 * Responsibilities:
 * - Create refresh token cookies with proper security settings
 * - Clear cookies on logout
 * - Apply consistent cookie configuration
 * <p>
 * Why Separate Service?
 * - Single Responsibility Principle
 * - Avoid code duplication (was in JwtUtils and RefreshTokenService)
 * - Easy to test (mock cookie creation)
 * - Easy to modify (change cookie logic in one place)
 * <p>
 * Used By:
 * - RefreshTokenService (create/clear cookies)
 * - AuthController (set cookies in response)
 * <p>
 * Cookie Security:
 * - HttpOnly: Prevents XSS attacks
 * - Secure: HTTPS only (production)
 * - SameSite: CSRF protection
 * - Domain: Controls cookie scope
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CookieService {

    private final CookieConfig cookieConfig;

    /**
     * Cookie name used throughout application
     * Consistent naming prevents cookie conflicts
     */
    private static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";

    /**
     * Create Refresh Token Cookie
     * <p>
     * Builds ResponseCookie with security settings from CookieConfig.
     * <p>
     * Cookie Properties:
     * - name: "refresh_token"
     * - value: The refresh token string
     * - httpOnly: From config (prevents JavaScript access)
     * - secure: From config (HTTPS only in production)
     * - sameSite: From config (CSRF protection)
     * - path: From config (usually "/")
     * - domain: From config (e.g., "localhost" or ".example.com")
     * - maxAge: Token lifetime in seconds
     * <p>
     * Security Features:
     * - HttpOnly prevents XSS token theft
     * - Secure prevents man-in-the-middle
     * - SameSite prevents CSRF attacks
     * - Automatic expiry (browser enforced)
     * <p>
     * Usage Flow:
     * 1. User logs in
     * 2. RefreshTokenService creates refresh token
     * 3. This method creates cookie
     * 4. Controller adds cookie to response
     * 5. Browser stores cookie
     * 6. Browser auto-sends cookie with future requests
     * <p>
     * Example:
     * ResponseCookie cookie = cookieService.createRefreshTokenCookie(
     * "abc-123-def",  // token value
     * 2592000         // 30 days in seconds
     * );
     * response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
     *
     * @param value         Refresh token value (plain, not hashed)
     * @param maxAgeSeconds Cookie lifetime in seconds (e.g., 30 days = 2592000)
     * @return ResponseCookie ready to add to HTTP response
     */
    public ResponseCookie createRefreshTokenCookie(String value, long maxAgeSeconds) {
        log.debug("Creating refresh token cookie with maxAge: {} seconds", maxAgeSeconds);

        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, value)
                .httpOnly(cookieConfig.isHttpOnly())
                .secure(cookieConfig.isSecure())
                .sameSite(cookieConfig.getSameSite())
                .path(cookieConfig.getPath())
                .domain(cookieConfig.getDomain())
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                .build();

        log.debug("Cookie created: domain={}, secure={}, sameSite={}, maxAge={}s",
                cookieConfig.getDomain(),
                cookieConfig.isSecure(),
                cookieConfig.getSameSite(),
                maxAgeSeconds);

        return cookie;
    }

    /**
     * Clear Refresh Token Cookie
     * <p>
     * Creates cookie with same name but maxAge=0.
     * Browser deletes cookie when maxAge=0.
     * <p>
     * How Cookie Deletion Works:
     * - Can't directly "delete" a cookie
     * - Instead, send cookie with maxAge=0
     * - Browser interprets as "expire immediately"
     * - Browser removes expired cookie
     * <p>
     * Important Properties Must Match:
     * - name: Must match original ("refresh_token")
     * - path: Must match original ("/")
     * - domain: Must match original ("localhost")
     * <p>
     * If properties don't match:
     * - Browser treats as different cookie
     * - Original cookie stays
     * - Logout fails
     * <p>
     * Called During:
     * - User logout
     * - Token expiration
     * - Security breach detected
     * - Session invalidation
     * <p>
     * Usage Flow:
     * 1. User clicks logout
     * 2. RefreshTokenService revokes token
     * 3. This method creates deletion cookie
     * 4. Controller adds to response
     * 5. Browser deletes cookie
     * 6. User redirected to login
     * <p>
     * Example:
     * ResponseCookie clearCookie = cookieService.clearRefreshTokenCookie();
     * response.addHeader(HttpHeaders.SET_COOKIE, clearCookie.toString());
     *
     * @return ResponseCookie that clears the refresh token cookie
     */
    public ResponseCookie clearRefreshTokenCookie() {
        log.debug("Creating cookie deletion directive");

        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
                .httpOnly(cookieConfig.isHttpOnly())
                .secure(cookieConfig.isSecure())
                .sameSite(cookieConfig.getSameSite())
                .path(cookieConfig.getPath())
                .domain(cookieConfig.getDomain())
                .maxAge(0)  // Expire immediately
                .build();

        log.debug("Cookie deletion directive created for domain: {}", cookieConfig.getDomain());

        return cookie;
    }

    /**
     * Get Cookie Name
     * <p>
     * Returns the standard cookie name used in the application.
     * <p>
     * Useful for:
     * - Extracting cookie from request
     * - Testing
     * - Logging
     * <p>
     * Example:
     * String cookieName = cookieService.getCookieName();
     * Cookie cookie = Arrays.stream(request.getCookies())
     * .filter(c -> c.getName().equals(cookieName))
     * .findFirst()
     * .orElse(null);
     *
     * @return Cookie name ("refresh_token")
     */
    public String getCookieName() {
        return REFRESH_TOKEN_COOKIE_NAME;
    }

    /**
     * Validate Cookie Configuration
     * <p>
     * Checks if cookie configuration is production-ready.
     * <p>
     * Warnings Logged For:
     * - secure=false (should be true in production)
     * - sameSite=None without secure=true
     * - Missing domain configuration
     * <p>
     * Called At:
     * - Application startup
     * - Can be used in health checks
     *
     * @return true if configuration is valid, false otherwise
     */
    public boolean isConfigurationValid() {
        boolean isValid = true;

        // Check for insecure production settings
        if (!cookieConfig.isSecure()) {
            log.warn("Cookie secure flag is false. This is INSECURE for production!");
            isValid = false;
        }

        // SameSite=None requires Secure
        if ("None".equalsIgnoreCase(cookieConfig.getSameSite()) && !cookieConfig.isSecure()) {
            log.error("SameSite=None requires secure=true. Configuration is INVALID!");
            isValid = false;
        }

        // Check domain is set
        if (cookieConfig.getDomain() == null || cookieConfig.getDomain().isBlank()) {
            log.warn("Cookie domain is not set. Cookies may not work correctly.");
            isValid = false;
        }

        return isValid;
    }

    /**
     * Log Current Configuration
     * <p>
     * Logs cookie configuration for debugging.
     * Useful during startup or troubleshooting.
     * <p>
     * Called At:
     * - Application startup (via @PostConstruct in main class)
     * - Configuration changes
     */
    public void logConfiguration() {
        log.info("Cookie Configuration:");
        log.info("  - Name: {}", REFRESH_TOKEN_COOKIE_NAME);
        log.info("  - HttpOnly: {}", cookieConfig.isHttpOnly());
        log.info("  - Secure: {}", cookieConfig.isSecure());
        log.info("  - SameSite: {}", cookieConfig.getSameSite());
        log.info("  - Path: {}", cookieConfig.getPath());
        log.info("  - Domain: {}", cookieConfig.getDomain());
    }
}