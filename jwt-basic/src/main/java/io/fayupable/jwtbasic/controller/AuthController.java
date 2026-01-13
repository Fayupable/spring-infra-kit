package io.fayupable.jwtbasic.controller;

import io.fayupable.jwtbasic.request.LoginRequest;
import io.fayupable.jwtbasic.request.RegisterRequest;
import io.fayupable.jwtbasic.response.AuthResponse;
import io.fayupable.jwtbasic.response.MessageResponse;
import io.fayupable.jwtbasic.service.IAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication Controller
 * <p>
 * REST API endpoints for user authentication.
 * <p>
 * Base URL: /api/auth
 * <p>
 * Available endpoints:
 * - POST /api/auth/register - Register new user
 * - POST /api/auth/login    - Login and get JWT token
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
     * Request body:
     * {
     * "email": "user@example.com",
     * "password": "password123",
     * "username": "johndoe" (optional)
     * }
     * <p>
     * Success response (201 Created):
     * {
     * "message": "User registered successfully"
     * }
     * <p>
     * Error response (400 Bad Request):
     * {
     * "message": "Email is already in use"
     * }
     *
     * @param request Registration details
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
     * Login and get JWT token
     * <p>
     * Endpoint: POST /api/auth/login
     * <p>
     * Request body:
     * {
     * "email": "user@example.com",
     * "password": "password123"
     * }
     * <p>
     * Success response (200 OK):
     * {
     * "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
     * "type": "Bearer",
     * "email": "user@example.com",
     * "roles": ["ROLE_USER"]
     * }
     * <p>
     * Error response (401 Unauthorized):
     * {
     * "error": "Unauthorized",
     * "message": "Bad credentials"
     * }
     *
     * @param request Login credentials
     * @return JWT token and user info with 200 status
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login endpoint called for email: {}", request.getEmail());

        AuthResponse response = authService.login(request);

        return ResponseEntity.ok(response);
    }
}