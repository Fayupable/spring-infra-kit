package io.fayupable.jwtrefreshtoken.security.jwt;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT Authentication Entry Point
 * <p>
 * Handles authentication failures in REST API.
 * <p>
 * This class is called when:
 * - User tries to access protected endpoint without JWT token
 * - JWT token is invalid or expired
 * - JWT token is present but authentication fails
 * <p>
 * Traditional web apps redirect to login page.
 * REST APIs return JSON error response (this is what we do).
 * <p>
 * This provides a consistent error format for all authentication failures.
 */
@Component
@Slf4j
public class JwtAuthEntryPoint implements AuthenticationEntryPoint {

    /**
     * Handle Authentication Failure
     * <p>
     * Called by Spring Security when authentication fails.
     * Returns a JSON error response instead of redirecting.
     * <p>
     * Common scenarios:
     * 1. No Authorization header → "Full authentication is required"
     * 2. Invalid JWT token → "Invalid or expired token"
     * 3. Expired JWT token → "JWT token is expired"
     * 4. Account disabled → "User is disabled"
     * <p>
     * Response format:
     * {
     * "status": 401,
     * "error": "Unauthorized",
     * "message": "Full authentication is required",
     * "path": "/api/auth/test"
     * }
     * <p>
     * Why Jackson 3.x (tools.jackson)?
     * - Spring Boot 4.0.1 uses Jackson 3.x
     * - Package changed from com.fasterxml to tools.jackson
     *
     * @param request       HTTP request
     * @param response      HTTP response
     * @param authException The exception that caused authentication failure
     */
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException)
            throws IOException, ServletException {

        log.error("Unauthorized error at {}: {}", request.getServletPath(), authException.getMessage());

        // Set response content type to JSON
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        // Set HTTP status to 401 Unauthorized
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        // Build error response body
        final Map<String, Object> body = new HashMap<>();
        body.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        body.put("error", "Unauthorized");
        body.put("message", authException.getMessage());
        body.put("path", request.getServletPath());

        // Convert map to JSON and write to response
        // Using Jackson 3.x ObjectMapper (tools.jackson package)
        final ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(response.getOutputStream(), body);
    }
}