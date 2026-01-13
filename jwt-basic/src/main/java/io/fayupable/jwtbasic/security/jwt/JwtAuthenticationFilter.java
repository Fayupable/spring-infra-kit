package io.fayupable.jwtbasic.security.jwt;

import io.fayupable.jwtbasic.security.user.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter
 * <p>
 * This filter runs on EVERY request to:
 * 1. Extract JWT token from Authorization header
 * 2. Validate the token
 * 3. Load user details from database
 * 4. Set authentication in Spring Security context
 * <p>
 * Extends OncePerRequestFilter to ensure it runs only once per request.
 */
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final UserDetailsServiceImpl userDetailsService;

    public JwtAuthenticationFilter(JwtUtils jwtUtils, UserDetailsServiceImpl userDetailsService) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Main filter method that runs on every request
     * <p>
     * Flow:
     * 1. Extract token from "Authorization: Bearer <token>" header
     * 2. If token exists and is valid:
     * - Load user from database
     * - Create authentication object
     * - Set it in SecurityContext
     * 3. Continue the filter chain
     *
     * @param request     HTTP request
     * @param response    HTTP response
     * @param filterChain Filter chain to continue
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            // Extract JWT token from Authorization header
            String jwt = extractTokenFromRequest(request);

            // If token exists and is valid, authenticate the user
            if (StringUtils.hasText(jwt) && jwtUtils.validateToken(jwt)) {
                // Get email from token
                String email = jwtUtils.getEmailFromToken(jwt);

                // Load user details from database
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                // Create authentication token
                // Parameters: principal, credentials, authorities
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,  // No credentials needed (already authenticated)
                                userDetails.getAuthorities()
                        );

                // Set additional details (IP address, session ID, etc.)
                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // Set authentication in Spring Security context
                // This makes the user "logged in" for this request
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("Authenticated user: {}", email);
            }

        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
        }

        // Continue with the filter chain
        filterChain.doFilter(request, response);
    }

    /**
     * Extract JWT token from Authorization header
     * <p>
     * Expected header format: "Authorization: Bearer <token>"
     *
     * @param request HTTP request
     * @return JWT token if found, null otherwise
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        // Check if header exists and starts with "Bearer "
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            // Remove "Bearer " prefix and return token
            return bearerToken.substring(7);
        }

        return null;
    }
}