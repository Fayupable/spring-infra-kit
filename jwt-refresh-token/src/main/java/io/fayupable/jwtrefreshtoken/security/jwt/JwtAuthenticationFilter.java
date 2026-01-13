package io.fayupable.jwtrefreshtoken.security.jwt;

import io.fayupable.jwtrefreshtoken.exception.AccountDisabledException;
import io.fayupable.jwtrefreshtoken.exception.AccountLockedException;
import io.fayupable.jwtrefreshtoken.exception.TokenBlacklistedException;
import io.fayupable.jwtrefreshtoken.exception.TokenExpiredException;
import io.fayupable.jwtrefreshtoken.security.user.UserDetailsServiceImpl;
import io.fayupable.jwtrefreshtoken.service.cookie.blacklist.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter (Production Grade)
 * <p>
 * This filter intercepts EVERY incoming HTTP request to perform comprehensive security checks.
 * <p>
 * Security Flow (Sequential Validation):
 * 1. Token Extraction: Get JWT from "Authorization: Bearer <token>" header
 * 2. Blacklist Validation: Check if token was revoked (logout işlemi sonrası)
 * 3. JWT Verification: Validate signature and expiration
 * 4. Identity Loading: Fetch user from database
 * 5. Account Status Enforcement: Verify account is ACTIVE and NOT LOCKED
 * 6. Create Authentication: Set up SecurityContext with user details
 * 7. Continue filter chain
 * <p>
 * Error Handling Strategy:
 * If ANY security check fails, throws a specific AuthenticationException.
 * These exceptions are delegated to JwtAuthEntryPoint for consistent JSON response.
 * <p>
 * Why OncePerRequestFilter?
 * - Ensures filter runs exactly once per request
 * - Prevents double execution in async/forwarded requests
 * - Spring framework best practice for security filters
 * <p>
 * Filter Order:
 * This filter runs BEFORE UsernamePasswordAuthenticationFilter
 * Configured in SecurityConfig.addFilterBefore()
 * <p>
 * Important:
 * - This filter now VALIDATES all security aspects proactively
 * - If any check fails, request is blocked via AuthenticationException
 * - JwtAuthEntryPoint handles proper error response formatting
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final UserDetailsServiceImpl userDetailsService;
    private final TokenBlacklistService tokenBlacklistService;
    private final JwtAuthEntryPoint authEntryPoint;

    /**
     * Main Filter Method
     * <p>
     * Called for every HTTP request.
     * <p>
     * Flow:
     * 1. Extract JWT token from Authorization header
     * 2. If token exists:
     * a. Validate blacklist status
     * b. Validate JWT signature & expiration
     * c. Load user from database
     * d. Validate account status (enabled, not locked)
     * e. Create Authentication object
     * f. Set authentication in SecurityContext
     * 3. If any validation fails, throw AuthenticationException
     * 4. Continue filter chain
     * <p>
     * Error Handling Approach:
     * - Instead of silent failures, we proactively validate all security aspects
     * - Specific exceptions provide clear feedback to clients
     * - JwtAuthEntryPoint converts exceptions to JSON responses
     * - Separation of concerns: filter validates, EntryPoint formats response
     * <p>
     * This approach ensures:
     * - No compromised tokens can be used (blacklist check)
     * - Expired tokens are rejected (JWT validation)
     * - Disabled/locked accounts cannot access (account status check)
     * - Clear error messages for debugging and client feedback
     *
     * @param request     HTTP request
     * @param response    HTTP response
     * @param filterChain Filter chain to continue processing
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // Step 1: Extract JWT token from Authorization header
            String jwt = extractTokenFromRequest(request);

            // Step 2: If token exists, perform sequential security checks
            if (StringUtils.hasText(jwt)) {
                authenticateRequest(jwt, request);
            }

            // Step 3: ALWAYS continue the filter chain
            // Even if token doesn't exist, public endpoints should proceed
            filterChain.doFilter(request, response);

        } catch (AuthenticationException e) {
            // Security validation failed
            // Log the failure and delegate to EntryPoint for JSON response
            // EntryPoint will return 401 Unauthorized with error details
            log.error("Authentication security validation failed: {}", e.getMessage());
            authEntryPoint.commence(request, response, e);
        }
    }

    /**
     * Performs Sequential Authentication and Authorization Checks
     * <p>
     * This method validates security requirements in a specific order.
     * Each check is critical and failures are reported immediately.
     * <p>
     * Validation Order (Sequential):
     * 1. Blacklist Check: Token revoked by logout?
     * 2. JWT Validation: Token signature valid? Not expired?
     * 3. User Loading: User exists in database?
     * 4. Account Status: Account enabled? Not locked?
     * 5. Context Setup: Create authentication and set in SecurityContext
     * <p>
     * Why Sequential Order?
     * - Early termination saves database queries (blacklist check first)
     * - Most common failures handled earliest
     * - Clear error reporting for each failure type
     *
     * @param jwt     The raw JWT string extracted from header
     * @param request Current HttpServletRequest (needed for authentication details)
     * @throws AuthenticationException if any security check fails
     */
    private void authenticateRequest(String jwt, HttpServletRequest request) {

        // ============ CHECK 1: Blacklist Validation ============
        // Purpose: Prevent use of tokens belonging to users who have logged out
        // Use Case: User logout should immediately invalidate their token
        // Implementation: Token stored in Redis/Database after logout
        // Performance: O(1) lookup in cache
        if (tokenBlacklistService.isBlacklisted(jwt)) {
            log.warn("Blacklisted token usage attempt detected. User may have logged out.");
            throw new TokenBlacklistedException("Token has been revoked. Please login again.");
        }

        // ============ CHECK 2: JWT Structural & Expiry Validation ============
        // Purpose: Verify JWT integrity, signature, and expiration
        // What it checks:
        // - Signature is valid (token not tampered with)
        // - Expiration date has not passed
        // - Token structure is correct
        // Note: This uses jwtUtils.validateToken() which verifies signature
        if (!jwtUtils.validateToken(jwt)) {
            log.warn("Invalid or expired JWT token provided in request.");
            throw new TokenExpiredException("JWT token is invalid or expired.");
        }

        // ============ CHECK 3: User Identity Loading ============
        // Purpose: Extract user information from token and load from database
        // Flow:
        // 1. Extract email from token payload
        // 2. Query database for user by email
        // 3. Load user's roles (EAGER fetch configured)
        // 4. UserDetails provides authorities (roles/permissions)
        String email = jwtUtils.getEmailFromToken(jwt);
        log.debug("Valid JWT token found for user: {}", email);

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        // ============ CHECK 4: Proactive Account Status Validation ============
        // Purpose: Ensure user hasn't been banned or disabled since token issuance
        // Why necessary?
        // - Token issued when account was active
        // - Admin may have disabled/locked account after token issuance
        // - Must reject requests from disabled accounts immediately
        // This also loads user's roles (EAGER fetch)
        validateUserAccountStatus(userDetails);

        // ============ STEP 5: Authentication Context Setup ============
        // Purpose: Create authentication object and set in SecurityContext
        // Flow:
        // 1. Create UsernamePasswordAuthenticationToken
        // 2. Set principal (user details with roles)
        // 3. Set credentials to null (JWT already validated, no password needed)
        // 4. Set authorities (user's roles/permissions)

        // Create authentication token
        // Parameters:
        // 1. principal: the user (UserDetails object)
        // 2. credentials: null (already authenticated via JWT validation)
        // 3. authorities: user's roles/permissions from database
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,  // No credentials needed, JWT already validated
                        userDetails.getAuthorities()
                );

        // Add request details to authentication
        // This includes IP address, session ID, servlet path, etc.
        // Useful for audit logs, security checks, and request tracking
        authentication.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request)
        );

        // Set authentication in Spring Security context
        // This makes the user "authenticated" for this request
        // Controllers can now access user via SecurityContextHolder
        // @AuthenticationPrincipal works, hasRole() authorization checks work
        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.debug("Authentication set for user: {}", email);
    }

    /**
     * Validates User Account Status and Flags
     * <p>
     * Checks if account is in a valid state for authentication.
     * Throws specific exceptions for clear error messages.
     * <p>
     * Account Status Flags:
     * 1. isEnabled(): Account active? (not disabled by admin)
     * 2. isAccountNonLocked(): Account not suspended? (not locked/banned)
     * <p>
     * Why These Checks?
     * - Admin may disable account after token was issued
     * - Admin may lock/ban account after token was issued
     * - Must reject immediately with clear error message
     * - Prevents disabled/locked accounts from accessing resources
     * <p>
     * Why Throw Specific Exceptions?
     * - Clear differentiation between disabled vs locked
     * - Client gets specific error message for each case
     * - JwtAuthEntryPoint formats these into JSON responses
     * - Helps debugging and user support
     *
     * @param userDetails The loaded security user from database
     * @throws AccountDisabledException if account is disabled
     * @throws AccountLockedException if account is locked/suspended
     */
    private void validateUserAccountStatus(UserDetails userDetails) {

        // Check if account is ACTIVE (not disabled)
        // isEnabled() returns false if admin has disabled the account
        // Use case: Account deactivation should immediately revoke token access
        if (!userDetails.isEnabled()) {
            log.warn("Disabled account authentication attempt.");
            throw new AccountDisabledException("Account is disabled. Verification required.");
        }

        // Check if account is NOT LOCKED/BANNED/SUSPENDED
        // isAccountNonLocked() returns false if account is locked
        // Use case: Account ban/suspension should immediately revoke token access
        if (!userDetails.isAccountNonLocked()) {
            log.warn("Locked/suspended account authentication attempt.");
            throw new AccountLockedException("Account is locked/suspended. Contact support.");
        }
    }

    /**
     * Extract JWT Token from Request
     * <p>
     * Looks for "Authorization" header with "Bearer " prefix.
     * <p>
     * Expected header format:
     * Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
     * <p>
     * Header Structure:
     * - Header name: "Authorization" (case-insensitive in HTTP)
     * - Scheme: "Bearer " (includes space, 7 characters total)
     * - Token: JWT string without spaces
     * <p>
     * Why "Bearer"?
     * - Standard OAuth 2.0 token type
     * - Indicates token-based authentication (not Basic auth, Digest, etc.)
     * - RFC 6750 defines Bearer token usage
     * <p>
     * What This Method Does:
     * 1. Get "Authorization" header value
     * 2. Check if header exists and has text
     * 3. Check if header starts with "Bearer "
     * 4. Extract token (remove "Bearer " prefix)
     * 5. Return token or null if not found/invalid format
     *
     * @param request HTTP request
     * @return JWT token string (without "Bearer " prefix), or null if not found
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        // Check if header exists and starts with "Bearer "
        // StringUtils.hasText() checks for null and empty strings
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            // Remove "Bearer " prefix (7 characters: "Bearer " = 6 letters + 1 space)
            return bearerToken.substring(7);
        }

        // Header doesn't exist or doesn't have "Bearer " prefix
        return null;
    }
}
