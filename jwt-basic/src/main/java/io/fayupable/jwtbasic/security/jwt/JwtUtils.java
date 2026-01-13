package io.fayupable.jwtbasic.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT Utility Class
 * <p>
 * This class handles all JWT token operations:
 * - Token generation
 * - Token validation
 * - Extracting information from tokens
 * <p>
 * Uses JJWT library (io.jsonwebtoken) version 0.12.x for JWT operations.
 */
@Component
@Slf4j
public class JwtUtils {

    /**
     * Secret key for signing JWT tokens
     * Loaded from application.yml
     * <p>
     * IMPORTANT: Generate using: openssl rand -base64 32
     * Never commit real secrets to version control!
     */
    @Value("${auth.token.jwtSecret}")
    private String jwtSecret;

    /**
     * Token expiration time in milliseconds
     * Default: 900000ms = 15 minutes
     */
    @Value("${auth.token.expirationInMils}")
    private int jwtExpirationTime;

    /**
     * Generate JWT token for authenticated user
     * <p>
     * Token contains:
     * - Subject: user's email
     * - Claim "roles": list of user roles
     * - Issued at: current timestamp
     * - Expiration: current time + expiration time
     *
     * @param authentication Spring Security Authentication object
     * @return JWT token as String
     */
    public String generateTokenForUser(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // Extract roles from authorities
        assert userDetails != null;
        List<String> roles = userDetails.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return Jwts.builder()
                .subject(userDetails.getUsername())      // Email as subject
                .claim("roles", roles)                    // Add roles to token
                .issuedAt(new Date())                    // Current timestamp
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationTime))
                .signWith(getSigningKey())               // Sign with secret (algorithm auto-detected)
                .compact();
    }

    /**
     * Validate JWT token
     * <p>
     * Checks:
     * - Token signature is valid
     * - Token is not expired
     * - Token format is correct
     *
     * @param token JWT token to validate
     * @return true if valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;

        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (JwtException e) {
            log.error("JWT token validation error: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }

        return false;
    }

    /**
     * Extract email (username) from JWT token
     *
     * @param token JWT token
     * @return User's email
     */
    public String getEmailFromToken(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * Extract roles from JWT token
     *
     * @param token JWT token
     * @return List of role names
     */
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        return getClaims(token).get("roles", List.class);
    }

    /**
     * Parse JWT token and extract claims
     * <p>
     * Claims contain all the information we stored in the token:
     * - Subject (email)
     * - Roles
     * - Expiration time
     * - Issued at time
     *
     * @param token JWT token
     * @return Claims object
     */
    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Get the signing key from base64-encoded secret
     * <p>
     * We decode the base64 secret and create an HMAC-SHA key.
     * This key is used to sign and verify JWT tokens.
     *
     * @return Signing key
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}