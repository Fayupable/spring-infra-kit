package io.fayupable.jwtrefreshtoken.service.auth;

import io.fayupable.jwtrefreshtoken.request.LoginRequest;
import io.fayupable.jwtrefreshtoken.request.RegisterRequest;
import io.fayupable.jwtrefreshtoken.response.AuthResponse;
import io.fayupable.jwtrefreshtoken.response.LogoutResponse;
import io.fayupable.jwtrefreshtoken.response.MessageResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;

/**
 * Authentication Service Interface
 * <p>
 * Defines core authentication operations.
 * Uses ClientInfoUtils internally for IP/Device tracking.
 */
public interface IAuthService {

    /**
     * Register a new user
     */
    MessageResponse register(RegisterRequest request);

    /**
     * Authenticate user (Login)
     * <p>
     * Supports "Smart Login" (Username or Email).
     * Automagically captures IP and User-Agent.
     */
    AuthResponse login(LoginRequest request);

    /**
     * Logout user
     * <p>
     * Extracts Access Token from request header internally.
     */
    LogoutResponse logout(HttpServletRequest request, String refreshToken);

    // Cookie Helpers (Delegates to RefreshTokenService/CookieService)
    ResponseCookie createRefreshTokenCookie(String refreshToken);

    ResponseCookie clearRefreshTokenCookie();
}