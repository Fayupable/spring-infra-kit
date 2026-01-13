package io.fayupable.jwtrefreshtoken.controller.auth;

import io.fayupable.jwtrefreshtoken.request.LoginRequest;
import io.fayupable.jwtrefreshtoken.request.RegisterRequest;
import io.fayupable.jwtrefreshtoken.response.AuthResponse;
import io.fayupable.jwtrefreshtoken.response.LogoutResponse;
import io.fayupable.jwtrefreshtoken.response.MessageResponse;
import io.fayupable.jwtrefreshtoken.response.RefreshTokenResponse;
import io.fayupable.jwtrefreshtoken.service.auth.IAuthService;
import io.fayupable.jwtrefreshtoken.service.refreshtoken.IRefreshTokenService;
import io.fayupable.jwtrefreshtoken.util.ClientInfoUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication Controller
 * <p>
 * REST API endpoints for user authentication and token management.
 * Handles the HTTP layer (Request/Response) and delegates logic to services.
 * <p>
 * Base URL: /api/auth
 * <p>
 * Features:
 * - Cookie-based Refresh Token (HttpOnly, Secure)
 * - Header-based Access Token (Bearer)
 * - IP and Device tracking integration
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final IAuthService authService;

    /**
     * Register a new user
     * <p>
     * Endpoint: POST /api/auth/register
     * <p>
     * Success response (201 Created):
     * {
     * "message": "User registered successfully!"
     * }
     *
     * @param request Registration details (email, password, username)
     * @return Success message with 201 status
     */
    @PostMapping("/register")
    public ResponseEntity<MessageResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Register endpoint called for email: {}", request.getEmail());

        MessageResponse response = authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * Login and get tokens
     * <p>
     * Endpoint: POST /api/auth/login
     * <p>
     * This endpoint does two things:
     * 1. Returns Access Token in JSON Body (for frontend usage)
     * 2. Sets Refresh Token in HttpOnly Cookie (for security)
     * <p>
     * Success response (200 OK):
     * Header: Set-Cookie: refresh_token=...; HttpOnly; Secure...
     * Body: { "accessToken": "...", "type": "Bearer", ... }
     *
     * @param request Login credentials (username/email + password)
     * @return Auth response with Cookie Header
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login endpoint called for identifier: {}", request.getUsernameOrEmail());

        // 1. Business Logic: Authenticate and generate tokens
        AuthResponse response = authService.login(request);

        // 2. Cookie Logic: Create secure cookie using the delegate method
        // This is why we added createRefreshTokenCookie() to AuthService!
        ResponseCookie jwtCookie = authService.createRefreshTokenCookie(response.refreshToken());

        // 3. Response: Return Body + Cookie Header
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .body(response);
    }



    /**
     * Logout User
     * <p>
     * Endpoint: POST /api/auth/logout
     * <p>
     * Actions:
     * 1. Revoke Refresh Token in DB
     * 2. Blacklist Access Token in Redis
     * 3. Clear HttpOnly Cookie from browser
     *
     * @param refreshToken From Cookie
     * @param request      To extract Access Token header
     * @return Logout success message + Clear Cookie Header
     */
    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletRequest request) {

        log.info("Logout endpoint called");

        // Logic delegates to AuthService
        LogoutResponse response = authService.logout(request, refreshToken);

        ResponseCookie cleanCookie = authService.clearRefreshTokenCookie();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cleanCookie.toString())
                .body(response);
    }

}