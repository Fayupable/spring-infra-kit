package io.fayupable.jwtrefreshtoken.controller.token;

import io.fayupable.jwtrefreshtoken.response.MessageResponse;
import io.fayupable.jwtrefreshtoken.response.RefreshTokenResponse;
import io.fayupable.jwtrefreshtoken.service.auth.IAuthService;
import io.fayupable.jwtrefreshtoken.service.refreshtoken.IRefreshTokenService;
import io.fayupable.jwtrefreshtoken.util.ClientInfoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Refresh Token Controller
 * <p>
 * Dedicated controller for Refresh Token lifecycle management.
 * Separating this from AuthController follows "Single Responsibility Principle".
 * <p>
 * Base URL: /api/token
 * <p>
 * Endpoints:
 * - POST /refresh  : Rotate token (Get new Access & Refresh Token)
 * - POST /validate : Check if token is valid (Boolean/Status)
 * - POST /revoke   : Invalidate specific token (Manual logout/cleanup)
 */
@Slf4j
@RestController
@RequestMapping("/api/token")
@RequiredArgsConstructor
public class RefreshTokenController {

    private final IRefreshTokenService refreshTokenService;

    /**
     * Refresh Access Token (Rotation)
     * <p>
     * Endpoint: POST /api/token/refresh
     * <p>
     * Uses the HttpOnly cookie to get a new Access Token.
     * Implements "Token Rotation" (old refresh token is replaced).
     * <p>
     * Flow:
     * 1. Validate old token
     * 2. Generate new tokens
     * 3. Revoke old token
     * 4. Return new tokens (Cookie + Body)
     *
     * @param refreshToken From HttpOnly cookie
     * @return New Access Token + New Refresh Token Cookie
     */
    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponse> refreshToken(
            @CookieValue(name = "refresh_token", required = false) String refreshToken) {

        log.info("Token refresh (rotation) request received");

        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new RuntimeException("Refresh Token is missing in cookie!");
        }

        // 1. Business Logic: Rotate tokens (Validate -> Generate New -> Revoke Old)
        // Uses ClientInfoUtils to capture IP and User-Agent automatically
        RefreshTokenResponse response = refreshTokenService.refreshToken(
                refreshToken,
                ClientInfoUtils.getUserAgent(),
                ClientInfoUtils.getClientIpAddress()
        );

        // 2. Cookie Logic: Create new secure cookie
        ResponseCookie jwtCookie = refreshTokenService.createRefreshTokenCookie(response.refreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .body(response);
    }

    /**
     * Validate Refresh Token
     * <p>
     * Endpoint: POST /api/token/validate
     * <p>
     * Checks if the refresh token is valid (exists, not expired, not revoked).
     * Does NOT rotate the token.
     * <p>
     * Use Case:
     * - Frontend checks if session is still valid on app startup
     * - Gateway checks token status before proxying
     *
     * @param refreshToken From HttpOnly cookie
     * @return 200 OK if valid, 400/401 if invalid
     */
    @PostMapping("/validate")
    public ResponseEntity<MessageResponse> validateToken(
            @CookieValue(name = "refresh_token", required = false) String refreshToken) {

        log.info("Token validation request received");

        if (refreshToken == null || refreshToken.isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Token missing"));
        }

        // 1. Logic: Check validity (Throws exception if invalid)
        refreshTokenService.validateTokenOnly(refreshToken);

        return ResponseEntity.ok(new MessageResponse("Token is valid"));
    }

    /**
     * Revoke Token (Manual)
     * <p>
     * Endpoint: POST /api/token/revoke
     * <p>
     * Manually invalidates the refresh token in the cookie.
     * Similar to logout but specific to the token controller context.
     *
     * @param refreshToken From HttpOnly cookie
     * @return Success message + Cleared Cookie
     */
    @PostMapping("/revoke")
    public ResponseEntity<MessageResponse> revokeToken(
            @CookieValue(name = "refresh_token", required = false) String refreshToken) {

        log.info("Token revocation request received");

        if (refreshToken != null && !refreshToken.isEmpty()) {
            // 1. Logic: Revoke token in DB
            refreshTokenService.revokeToken(refreshToken);
        }

        // 2. Cookie Logic: Clear the cookie from browser
        ResponseCookie cleanCookie = refreshTokenService.clearRefreshTokenCookie();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cleanCookie.toString())
                .body(new MessageResponse("Token revoked successfully"));
    }
}