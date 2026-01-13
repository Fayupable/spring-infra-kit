package io.fayupable.jwtrefreshtoken.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Cookie Configuration Properties
 * <p>
 * Loads cookie settings from application.yml.
 * Provides centralized cookie configuration for the application.
 * <p>
 * Configuration Binding:
 * Spring Boot automatically binds properties from YAML to this class.
 * <p>
 * YAML Structure:
 * auth:
 * cookie:
 * http-only: true
 * secure: false
 * same-site: Lax
 * path: /
 * domain: localhost
 * <p>
 * How It Works:
 * 1. @ConfigurationProperties(prefix = "auth.cookie") tells Spring:
 * "Bind all properties under auth.cookie.* to this class"
 * <p>
 * 2. Spring looks for:
 * - auth.cookie.http-only → setHttpOnly()
 * - auth.cookie.secure → setSecure()
 * - auth.cookie.same-site → setSameSite()
 * - auth.cookie.path → setPath()
 * - auth.cookie.domain → setDomain()
 * <p>
 * 3. Naming Convention:
 * - YAML: kebab-case (http-only, same-site)
 * - Java: camelCase (httpOnly, sameSite)
 * - Spring automatically converts between them
 * <p>
 * Default Values:
 * - Only used if property is NOT in YAML
 * - YAML values ALWAYS override defaults
 * <p>
 * Usage:
 * - RefreshTokenService uses this via CookieService
 * - All cookie properties configured in one place
 * - Easy to switch between dev/prod settings
 * <p>
 * Security Notes:
 * - httpOnly: Prevents JavaScript access (XSS protection)
 * - secure: HTTPS only (man-in-the-middle protection)
 * - sameSite: CSRF protection
 */
@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "auth.cookie")
public class CookieConfig {

    /**
     * HttpOnly Flag
     * <p>
     * Controls JavaScript access to cookie.
     * <p>
     * When true:
     * - JavaScript cannot read cookie via document.cookie
     * - Prevents XSS attacks from stealing token
     * - Browser automatically sends cookie with requests
     * <p>
     * When false:
     * - JavaScript CAN read cookie (vulnerable to XSS)
     * - Only use for debugging
     * <p>
     * YAML Property: auth.cookie.http-only
     * Environment Variable: COOKIE_HTTP_ONLY
     * <p>
     * Default: true (safe default if not in YAML)
     * Production: MUST be true
     */
    private boolean httpOnly;

    /**
     * Secure Flag
     * <p>
     * Controls HTTPS requirement for cookie.
     * <p>
     * When true:
     * - Cookie only sent over HTTPS
     * - Prevents man-in-the-middle attacks
     * - Token can't be intercepted on network
     * <p>
     * When false:
     * - Cookie sent over HTTP (DANGEROUS in production!)
     * - Acceptable for local development
     * <p>
     * YAML Property: auth.cookie.secure
     * Environment Variable: COOKIE_SECURE
     * <p>
     * Default: false (allows local development)
     * Production: MUST be true
     */
    private boolean secure;

    /**
     * SameSite Attribute
     * <p>
     * Controls when cookie is sent with cross-site requests.
     * <p>
     * Options:
     * - "Strict": Never sent with cross-site requests (most secure)
     * - "Lax": Sent with top-level navigation (recommended)
     * - "None": Sent with all requests (requires secure=true)
     * <p>
     * Recommendations:
     * - Use "Lax" for most cases (good balance)
     * - Use "Strict" for high-security apps
     * - Use "None" only if you need cross-site cookies (requires HTTPS)
     * <p>
     * YAML Property: auth.cookie.same-site
     * Environment Variable: COOKIE_SAME_SITE
     * <p>
     * Default: "Lax" (safe default)
     */
    private String sameSite;

    /**
     * Cookie Path
     * <p>
     * Defines which URLs receive the cookie.
     * <p>
     * Examples:
     * - "/" → Cookie sent to all paths on domain
     * - "/api" → Cookie only sent to /api/* paths
     * - "/auth" → Cookie only sent to /auth/* paths
     * <p>
     * Recommendation:
     * - Use "/" for most cases (cookie available everywhere)
     * - Use specific path only if you have multiple apps on same domain
     * <p>
     * YAML Property: auth.cookie.path
     * Environment Variable: COOKIE_PATH
     * <p>
     * Default: "/"
     */
    private String path;

    /**
     * Cookie Domain
     * <p>
     * Defines which domains can receive cookie.
     * <p>
     * Examples:
     * - "localhost" → Only localhost (local development)
     * - ".example.com" → example.com and ALL subdomains
     * - "api.example.com" → Only this specific subdomain
     * <p>
     * Leading Dot Behavior:
     * - ".example.com" → Includes api.example.com, www.example.com, etc.
     * - "example.com" → Only example.com (NO subdomains)
     * <p>
     * Recommendations:
     * - Local: "localhost"
     * - Production (with subdomains): ".yourdomain.com"
     * - Production (single domain): "yourdomain.com"
     * <p>
     * YAML Property: auth.cookie.domain
     * Environment Variable: COOKIE_DOMAIN
     * <p>
     * Default: "localhost" (safe for development)
     */
    private String domain;
}