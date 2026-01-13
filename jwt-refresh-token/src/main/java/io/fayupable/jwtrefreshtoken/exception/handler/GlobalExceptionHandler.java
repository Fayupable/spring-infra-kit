package io.fayupable.jwtrefreshtoken.exception.handler;

import io.fayupable.jwtrefreshtoken.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Global Exception Handler
 * <p>
 * Centralized exception handling for the entire application.
 * <p>
 * Responsibilities:
 * - Catch all exceptions thrown by controllers and services
 * - Convert exceptions to consistent JSON responses
 * - Log security events for monitoring
 * - Return appropriate HTTP status codes
 * <p>
 * Benefits:
 * - Consistent error format across entire API
 * - Single place to handle exceptions
 * - Clean controller code (no try-catch needed)
 * - Easy to modify error responses
 * - Centralized security logging
 * <p>
 * Response Format:
 * {
 * "timestamp": "2025-01-02T10:30:00",
 * "status": 401,
 * "error": "Unauthorized",
 * "message": "Token has expired"
 * }
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handle Token Expired Exception
     * <p>
     * Scenarios:
     * - Access token expired (15 min passed)
     * - Refresh token sliding window expired (30 days inactive)
     * - Refresh token absolute expiry reached (90 days max)
     * <p>
     * HTTP Status: 401 Unauthorized
     * <p>
     * Client Should:
     * - If access token expired: Try refresh endpoint
     * - If refresh token expired: Redirect to login page
     */
    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<Object> handleTokenExpiredException(TokenExpiredException e, WebRequest request) {
        log.warn("Token expired: {} | Path: {}", e.getMessage(), extractPath(request));
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    /**
     * Handle Token Blacklisted Exception
     * <p>
     * Scenarios:
     * - User logged out but token not expired yet
     * - Admin revoked session
     * - Password changed (all tokens blacklisted)
     * <p>
     * HTTP Status: 401 Unauthorized
     * <p>
     * Client Should:
     * - Clear all tokens
     * - Redirect to login page
     * - Show "Session expired" message
     */
    @ExceptionHandler(TokenBlacklistedException.class)
    public ResponseEntity<Object> handleTokenBlacklistedException(TokenBlacklistedException e, WebRequest request) {
        log.info("Blacklisted token used: {} | Path: {}", e.getMessage(), extractPath(request));
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Token has been revoked. Please login again.");
    }

    /**
     * Handle Token Reuse Exception
     * <p>
     * CRITICAL SECURITY EVENT
     * <p>
     * Scenarios:
     * - Attacker tries to use stolen token after rotation
     * - Replay attack detected
     * <p>
     * Response:
     * - Revoke ALL user tokens (done in service)
     * - Force re-login
     * - Log security event
     * <p>
     * HTTP Status: 401 Unauthorized
     * <p>
     * Monitoring:
     * In production, this should trigger security alerts
     */
    @ExceptionHandler(TokenReusedException.class)
    public ResponseEntity<Object> handleTokenReusedException(TokenReusedException e, WebRequest request) {
        log.error("SECURITY ALERT: Token reuse detected! {} | Path: {} | IP: {}",
                e.getMessage(),
                extractPath(request),
                extractIpAddress(request));

        return buildErrorResponse(
                HttpStatus.UNAUTHORIZED,
                "Security breach detected: Token reuse. All sessions revoked. Please login again."
        );
    }

    /**
     * Handle Account Disabled Exception
     * <p>
     * Scenarios:
     * - User status: PENDING_APPROVAL (email not verified)
     * - User status: INACTIVE (account deactivated)
     * <p>
     * HTTP Status: 403 Forbidden
     * <p>
     * Why 403 not 401:
     * - User credentials are valid (authenticated)
     * - But account is disabled (not authorized)
     * - 401 = wrong credentials
     * - 403 = correct credentials but no access
     */
    @ExceptionHandler({AccountDisabledException.class, DisabledException.class})
    public ResponseEntity<Object> handleAccountDisabledException(Exception e, WebRequest request) {
        log.warn("Disabled account access attempt: {} | Path: {}", e.getMessage(), extractPath(request));
        return buildErrorResponse(
                HttpStatus.FORBIDDEN,
                "Account is disabled. Please verify your email or contact support."
        );
    }

    /**
     * Handle Account Locked Exception
     * <p>
     * Scenarios:
     * - User status: BANNED (permanent lock)
     * - User status: SUSPENDED (temporary lock)
     * - Too many failed login attempts
     * <p>
     * HTTP Status: 403 Forbidden
     * <p>
     * Security Note:
     * Log for monitoring. Banned users trying to access = suspicious activity
     */
    @ExceptionHandler({AccountLockedException.class, LockedException.class})
    public ResponseEntity<Object> handleAccountLockedException(Exception e, WebRequest request) {
        log.warn("Locked account access attempt: {} | Path: {} | IP: {}",
                e.getMessage(),
                extractPath(request),
                extractIpAddress(request));

        return buildErrorResponse(
                HttpStatus.FORBIDDEN,
                "Account is locked. Please contact support."
        );
    }

    /**
     * Handle Bad Credentials Exception
     * <p>
     * Scenarios:
     * - Wrong email or password during login
     * - Invalid credentials
     * <p>
     * HTTP Status: 401 Unauthorized
     * <p>
     * Security Note:
     * Generic message prevents username enumeration
     * Don't reveal if email exists or password is wrong
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Object> handleBadCredentialsException(BadCredentialsException e, WebRequest request) {
        log.warn("Bad credentials attempt | Path: {} | IP: {}",
                extractPath(request),
                extractIpAddress(request));

        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Invalid email or password.");
    }

    /**
     * Handle Generic Authentication Exception
     * <p>
     * Catches any AuthenticationException not handled above
     * <p>
     * HTTP Status: 401 Unauthorized
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Object> handleAuthenticationException(AuthenticationException e, WebRequest request) {
        log.warn("Authentication failed: {} | Path: {}", e.getMessage(), extractPath(request));
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    /**
     * Handle Generic Runtime Exception
     * <p>
     * Catches unexpected errors
     * <p>
     * HTTP Status: 500 Internal Server Error
     * <p>
     * Security Note:
     * Don't expose internal error details to client
     * Log full stack trace for debugging
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Object> handleRuntimeException(RuntimeException e, WebRequest request) {
        log.error("Unexpected error: {} | Path: {}",
                e.getMessage(),
                extractPath(request),
                e);

        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later."
        );
    }

    /**
     * Build Error Response
     * <p>
     * Creates consistent error response format
     * <p>
     * Format:
     * {
     * "timestamp": "2025-01-02T10:30:00",
     * "status": 401,
     * "error": "Unauthorized",
     * "message": "Token has expired"
     * }
     */
    private ResponseEntity<Object> buildErrorResponse(HttpStatus status, String message) {
        Map<String, Object> errorDetails = new LinkedHashMap<>();
        errorDetails.put("timestamp", LocalDateTime.now());
        errorDetails.put("status", status.value());
        errorDetails.put("error", status.getReasonPhrase());
        errorDetails.put("message", message);

        return new ResponseEntity<>(errorDetails, status);
    }

    /**
     * Extract Request Path
     * <p>
     * Gets clean path from WebRequest
     */
    private String extractPath(WebRequest request) {
        String description = request.getDescription(false);
        return description.replace("uri=", "");
    }

    /**
     * Extract IP Address
     * <p>
     * Gets client IP from request
     * Checks X-Forwarded-For header if behind proxy
     */
    private String extractIpAddress(WebRequest request) {
        String header = request.getHeader("X-Forwarded-For");
        if (header != null && !header.isEmpty()) {
            return header.split(",")[0].trim();
        }
        return "unknown";
    }
}