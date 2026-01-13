package io.fayupable.jwtrefreshtoken.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * JWT Utility Class
 * <p>
 * Handles all JWT token operations for both access and refresh tokens.
 * <p>
 * Token Types:
 * 1. Access Token:
 * - Short-lived (15 minutes)
 * - Contains: email, userId, roles, status
 * - Used for API authentication
 * <p>
 * 2. Refresh Token:
 * - Long-lived (30 days sliding, 90 days max)
 * - Contains: email, userId, tokenType=refresh
 * - Used only to get new access token
 * - Stored in database (hashed)
 * <p>
 * JJWT 0.12.x API (Spring Boot 4.0.1):
 * - .subject() instead of .setSubject()
 * - .issuedAt() instead of .setIssuedAt()
 * - .expiration() instead of .setExpiration()
 * - .signWith(key) instead of .signWith(key, algorithm)
 * - .verifyWith() instead of .setSigningKey()
 * - .parseSignedClaims() instead of .parseClaimsJws()
 * - .getPayload() instead of .getBody()
 */
@Component
@Slf4j
public class JwtUtils {

    /**
     * Secret key for signing JWT tokens
     * Generate with: openssl rand -base64 32
     */
    @Value("${auth.token.jwtSecret}")
    private String jwtSecret;

    /**
     * Access token expiration (15 minutes = 900000ms)
     */
    @Value("${auth.token.expirationInMils}")
    private long accessTokenExpiration;

    /**
     * Refresh token sliding window expiration (30 days = 2592000000ms)
     * Token validity extends each time it's used
     */
    @Getter
    @Value("${auth.token.refreshExpirationInMils}")
    private long refreshTokenExpiration;

    /**
     * Refresh token absolute max lifetime (90 days = 7776000000ms)
     * Token MUST expire after this time regardless of usage
     */
    @Getter
    @Value("${auth.token.maxAbsoluteLifetimeInMils}")
    private long maxAbsoluteLifetime;

    /**
     * Generate Access Token from Authentication
     * <p>
     * Called after successful login.
     * Contains user info for authorization checks.
     *
     * @param authentication Spring Security Authentication object
     * @return JWT access token
     */
    public String generateAccessToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        assert userDetails != null;
        List<String> roles = userDetails.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return buildToken(
                userDetails.getUsername(),
                null,  // userId not available from Authentication
                roles,
                accessTokenExpiration,
                false  // not a refresh token
        );
    }

    /**
     * Generate Access Token with explicit parameters
     * <p>
     * Used when refreshing token (we have userId from database).
     *
     * @param email  User's email
     * @param userId User's ID
     * @param roles  User's roles
     * @return JWT access token
     */
    public String generateAccessToken(String email, String userId, List<String> roles) {
        return buildToken(email, userId, roles, accessTokenExpiration, false);
    }

    /**
     * Generate Refresh Token from Authentication
     * <p>
     * Called after successful login.
     * Returns plain token (NOT the hash).
     *
     * @param authentication Spring Security Authentication object
     * @return Plain refresh token (to be hashed before storing)
     */
    public String generateRefreshToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        assert userDetails != null;
        return buildToken(
                userDetails.getUsername(),
                null,
                null,  // No roles in refresh token
                refreshTokenExpiration,
                true   // is a refresh token
        );
    }

    /**
     * Generate Refresh Token with explicit parameters
     * <p>
     * Used during token rotation.
     *
     * @param email  User's email
     * @param userId User's ID
     * @return Plain refresh token (to be hashed before storing)
     */
    public String generateRefreshToken(String email, String userId) {
        return buildToken(email, userId, null, refreshTokenExpiration, true);
    }

    /**
     * Build JWT Token (Internal method)
     * <p>
     * Core token creation logic.
     * Handles both access and refresh tokens.
     *
     * @param email      User's email (subject)
     * @param userId     User's ID (claim)
     * @param roles      User's roles (claim, only for access tokens)
     * @param expiration Token expiration time in milliseconds
     * @param isRefresh  Is this a refresh token?
     * @return JWT token string
     */
    private String buildToken(String email,
                              String userId,
                              List<String> roles,
                              long expiration,
                              boolean isRefresh) {

        JwtBuilder builder = Jwts.builder()
                .subject(email)                                    // JJWT 0.12.x: .subject()
                .issuedAt(new Date())                             // JJWT 0.12.x: .issuedAt()
                .expiration(new Date(System.currentTimeMillis() + expiration))  // JJWT 0.12.x: .expiration()
                .signWith(getSigningKey())                        // JJWT 0.12.x: no algorithm param
                .header()
                .add("typ", "JWT")
                .and();

        // Add userId if provided
        if (userId != null) {
            builder.claim("id", userId);
        }

        // For refresh tokens: add tokenType claim
        if (isRefresh) {
            builder.claim("tokenType", "refresh");
        }
        // For access tokens: add roles claim
        else if (roles != null) {
            builder.claim("roles", roles);
        }

        return builder.compact();
    }

    /**
     * Validate JWT Token
     * <p>
     * Checks:
     * - Signature is valid
     * - Token is not expired
     * - Token format is correct
     *
     * @param token JWT token to validate
     * @return true if valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())          // JJWT 0.12.x: .verifyWith()
                    .build()
                    .parseSignedClaims(token);            // JJWT 0.12.x: .parseSignedClaims()
            return true;

        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token format: {}", e.getMessage());
        } catch (JwtException e) {
            log.error("JWT token validation error: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }

        return false;
    }

    /**
     * Extract Email from Token
     * <p>
     * Gets the subject (email) from token claims.
     *
     * @param token JWT token
     * @return User's email
     */
    public String getEmailFromToken(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * Extract User ID from Token
     * <p>
     * Gets the userId claim from token.
     * Returns trimmed string to avoid whitespace issues.
     *
     * @param token JWT token
     * @return User's ID as string
     */
    public String getUserIdFromToken(String token) {
        String userId = getClaims(token).get("id", String.class);
        return userId != null ? userId.trim() : null;
    }

    /**
     * Extract Roles from Token
     * <p>
     * Gets the roles claim from access token.
     * Refresh tokens don't have roles.
     *
     * @param token JWT token
     * @return List of role names
     */
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        return getClaims(token).get("roles", List.class);
    }

    /**
     * Check if Token is Refresh Token
     * <p>
     * Checks the tokenType claim.
     *
     * @param token JWT token
     * @return true if refresh token, false if access token
     */
    public boolean isRefreshToken(String token) {
        String tokenType = getClaims(token).get("tokenType", String.class);
        return "refresh".equals(tokenType);
    }

    /**
     * Parse Token and Extract Claims
     * <p>
     * Extracts all claims (data) from the token.
     *
     * @param token JWT token
     * @return Claims object containing all token data
     */
    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();                    // JJWT 0.12.x: .getPayload()
    }

    /**
     * Get Signing Key
     * <p>
     * Converts base64-encoded secret to SecretKey.
     * Uses HMAC-SHA algorithm for signing.
     *
     * @return SecretKey for signing/verifying tokens
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }


    /**
     * Extract JWT Token from HTTP Request
     * <p>
     * Helper method to extract the "Bearer" token from the "Authorization" header.
     * Centralizes extraction logic to avoid duplication in Filters and Controllers.
     *
     * @param request The incoming HTTP request
     * @return JWT string if found, null otherwise
     */
    public String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}