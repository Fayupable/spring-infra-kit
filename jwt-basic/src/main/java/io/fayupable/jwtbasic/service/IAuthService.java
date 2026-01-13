package io.fayupable.jwtbasic.service;

import io.fayupable.jwtbasic.request.LoginRequest;
import io.fayupable.jwtbasic.request.RegisterRequest;
import io.fayupable.jwtbasic.response.AuthResponse;
import io.fayupable.jwtbasic.response.MessageResponse;

/**
 * Authentication Service Interface
 *
 * Defines contract for authentication operations.
 *
 * Benefits of using interface:
 * - Easy to mock in tests
 * - Can swap implementations
 * - Clear contract for what service provides
 */
public interface IAuthService {

    /**
     * Register a new user
     *
     * @param request Registration details
     * @return Success message
     * @throws RuntimeException if email already exists or role not found
     */
    MessageResponse register(RegisterRequest request);

    /**
     * Authenticate user and generate JWT token
     *
     * @param request Login credentials
     * @return JWT token and user information
     * @throws org.springframework.security.core.AuthenticationException if credentials invalid
     */
    AuthResponse login(LoginRequest request);
}