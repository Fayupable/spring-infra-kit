package io.fayupable.jwtbasic.security.jwt;

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
 * This class handles what happens when an unauthenticated user
 * tries to access a protected resource.
 * <p>
 * Instead of redirecting to a login page (traditional web apps),
 * we return a JSON error response (REST API style).
 */
@Component
@Slf4j
public class JwtAuthEntryPoint implements AuthenticationEntryPoint {

    /**
     * Called when authentication fails
     * <p>
     * This happens when:
     * - No JWT token is provided
     * - JWT token is invalid or expired
     * - User tries to access protected endpoint without authentication
     *
     * @param request       HTTP request
     * @param response      HTTP response
     * @param authException The authentication exception that occurred
     */
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException)
            throws IOException, ServletException {

        log.error("Unauthorized error: {}", authException.getMessage());

        // Set response type to JSON
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        // Create error response body
        final Map<String, Object> body = new HashMap<>();
        body.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        body.put("error", "Unauthorized");
        body.put("message", authException.getMessage());
        body.put("path", request.getServletPath());

        // Convert map to JSON and write to response
        final ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(response.getOutputStream(), body);
    }
}