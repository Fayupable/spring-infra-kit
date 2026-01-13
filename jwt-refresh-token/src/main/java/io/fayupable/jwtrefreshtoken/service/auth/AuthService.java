package io.fayupable.jwtrefreshtoken.service.auth;

import io.fayupable.jwtrefreshtoken.entity.Role;
import io.fayupable.jwtrefreshtoken.entity.User;
import io.fayupable.jwtrefreshtoken.enums.RoleName;
import io.fayupable.jwtrefreshtoken.request.LoginRequest;
import io.fayupable.jwtrefreshtoken.request.RegisterRequest;
import io.fayupable.jwtrefreshtoken.response.AuthResponse;
import io.fayupable.jwtrefreshtoken.response.LogoutResponse;
import io.fayupable.jwtrefreshtoken.response.MessageResponse;
import io.fayupable.jwtrefreshtoken.repository.RoleRepository;
import io.fayupable.jwtrefreshtoken.repository.UserRepository;
import io.fayupable.jwtrefreshtoken.security.jwt.JwtUtils;
import io.fayupable.jwtrefreshtoken.security.user.UserDetailsImpl;
import io.fayupable.jwtrefreshtoken.service.refreshtoken.IRefreshTokenService;
import io.fayupable.jwtrefreshtoken.util.ClientInfoUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Authentication Service Implementation
 * <p>
 * Central orchestrator for the authentication flow.
 * Acts as a Facade layer between the Controller and lower-level components.
 * <p>
 * Core Responsibilities:
 * - User Registration (Sign up)
 * - User Authentication (Sign in)
 * - Session Termination (Sign out)
 * - Token Generation orchestration (Access + Refresh)
 * <p>
 * Key Integrations:
 * - Spring Security: Delegates actual authentication checks
 * - User Repository: Persists user data
 * - JWT Utils: Generates stateless access tokens
 * - RefreshTokenService: Manages stateful refresh tokens
 * <p>
 * Clean Architecture Note:
 * This service breaks down complex operations into small, readable private helper methods.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService implements IAuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final IRefreshTokenService refreshTokenService;


    /**
     * Register New User
     * <p>
     * Handles the complete sign-up process.
     * <p>
     * Flow:
     * 1. Validate constraints (check if email/username taken)
     * 2. Construct User entity (encode password)
     * 3. Assign default roles (ROLE_USER)
     * 4. Persist to database
     * <p>
     * Transactional:
     * Ensures all steps complete successfully or rollback.
     * Prevents partial data state (e.g., user created but no role).
     *
     * @param request Contains username, email, password
     * @return Success message response
     */
    @Override
    @Transactional
    public MessageResponse register(RegisterRequest request) {
        log.info("Registration attempt for email: {}", request.getEmail());

        // 1. Validate Input
        validateRegistrationRequest(request);

        // 2. Create User Entity
        User user = createUserEntity(request);

        // 3. Assign Default Roles
        assignDefaultRole(user);

        // 4. Persist User
        userRepository.save(user);

        log.info("User registered successfully: {}", user.getEmail());
        return new MessageResponse("User registered successfully!");
    }

    /**
     * Login User & Issue Tokens
     * <p>
     * The core authentication workflow.
     * Converts raw credentials into secure JWT tokens.
     * <p>
     * Process Flow:
     * 1. Authenticate: Verify credentials via Spring Security
     * 2. Generate Access Token: Create short-lived JWT (stateless)
     * 3. Generate Refresh Token: Create long-lived token (stateful)
     * 4. Store Session: Save refresh token to DB with device info
     * 5. Response: Return tokens and user info
     * <p>
     * Why two tokens?
     * - Access Token: Used for API authorization (User -> Server)
     * - Refresh Token: Used to get new Access Token (User -> Auth Server)
     *
     * @param request Contains username/email and password
     * @return AuthResponse with JWTs and user details
     */
    @Override
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for identifier: {}", request.getUsernameOrEmail());

        // 1. Authenticate (Spring Security)
        Authentication authentication = authenticateUser(request.getUsernameOrEmail(), request.getPassword());
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        // 2. Generate Tokens
        String accessToken = jwtUtils.generateAccessToken(authentication);
        String refreshToken = jwtUtils.generateRefreshToken(authentication);

        // 3. Persist Refresh Token (with IP/Agent)
        saveRefreshTokenToDatabase(Objects.requireNonNull(userDetails).getId(), refreshToken);

        log.info("Login successful for user: {}", userDetails.getEmail());

        // 4. Build and Return Response
        return buildAuthResponse(userDetails, accessToken, refreshToken);
    }

    /**
     * Logout User
     * <p>
     * Terminates the user's session securely.
     * <p>
     * Actions:
     * 1. Extract Access Token from request header
     * 2. Pass to RefreshTokenService to:
     * - Revoke Refresh Token in DB
     * - Blacklist Access Token in Redis
     * <p>
     *
     * @param request      HTTP request to extract Access Token
     * @param refreshToken Refresh token from cookie (to be revoked)
     * @return Success response
     */
    @Override
    public LogoutResponse logout(HttpServletRequest request, String refreshToken) {
        String accessToken = jwtUtils.getTokenFromRequest(request);
        return refreshTokenService.logout(refreshToken, accessToken);
    }

    // ==================================================================================
    // PRIVATE HELPER METHODS (Implementation Details)
    // ==================================================================================

    /**
     * Authenticates the user using Spring Security's AuthenticationManager.
     * <p>
     * This triggers the UserDetailsServiceImpl.loadUserByUsername() method.
     * If successful, it sets the Authentication object in the SecurityContext.
     *
     * @param identifier Username or Email
     * @param password   Raw password
     * @return Authentication object containing UserDetails
     */
    private Authentication authenticateUser(String identifier, String password) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(identifier, password)
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return authentication;
    }

    /**
     * Delegates the creation and saving of the refresh token to the RefreshTokenService.
     * <p>
     * Automatically extracts client metadata (IP, User-Agent) using
     * ClientInfoUtils to ensure tracking without cluttering the main logic.
     */
    private void saveRefreshTokenToDatabase(UUID userId, String refreshToken) {
        refreshTokenService.createRefreshToken(
                userId,
                refreshToken,
                ClientInfoUtils.getUserAgent(),
                ClientInfoUtils.getClientIpAddress()
        );
    }

    /**
     * Constructs the AuthResponse DTO.
     * <p>
     * Extracts roles from UserDetails internally to keep main logic clean.
     * Maps internal domain objects to API response format.
     */
    private AuthResponse buildAuthResponse(UserDetailsImpl userDetails, String accessToken, String refreshToken) {
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .type("Bearer")
                .email(userDetails.getEmail())
                .roles(roles)
                .expiresIn(jwtUtils.getRefreshTokenExpiration())
                .build();
    }

    /**
     * Validates if the email or username is already taken.
     * <p>
     * Throws RuntimeException if duplicates are found.
     * This is a second layer of defense after frontend validation.
     */
    private void validateRegistrationRequest(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Error: Email is already in use!");
        }
        if (request.getUsername() != null && userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Error: Username is already taken!");
        }
    }

    /**
     * Creates a new User entity from the request data.
     * <p>
     * - Determines correct username
     * - Encodes password using BCrypt
     */
    private User createUserEntity(RegisterRequest request) {
        return User.builder()
                .username(determineUsername(request))
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();
    }

    /**
     * Smart Username Determination.
     * <p>
     * If the user didn't provide a username, fallback to using their email
     * as the username. This supports "Email Only" registration flows.
     */
    private String determineUsername(RegisterRequest request) {
        return request.getUsername() != null ? request.getUsername() : request.getEmail();
    }

    /**
     * Assigns the default role to the user.
     * <p>
     * Standard Policy: All new registrations get ROLE_USER.
     * Admin roles must be assigned manually in DB or via Admin API.
     */
    private void assignDefaultRole(User user) {
        Set<Role> roles = new HashSet<>();
        Role userRole = roleRepository.findByRoleName(RoleName.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
        roles.add(userRole);
        user.setRoles(roles);
    }

    // ==================================================================================
    // COOKIE DELEGATES
    // ==================================================================================
    // These methods act as a bridge (Facade) to the RefreshTokenService/CookieService.
    // This keeps the Controller clean by allowing it to depend ONLY on AuthService,
    // rather than injecting multiple services just to set a header.

    /**
     * Delegate to create secure HTTP-only cookie for refresh token.
     */
    @Override
    public ResponseCookie createRefreshTokenCookie(String refreshToken) {
        return refreshTokenService.createRefreshTokenCookie(refreshToken);
    }

    /**
     * Delegate to create a "clean" (empty/expired) cookie for logout.
     */
    @Override
    public ResponseCookie clearRefreshTokenCookie() {
        return refreshTokenService.clearRefreshTokenCookie();
    }
}