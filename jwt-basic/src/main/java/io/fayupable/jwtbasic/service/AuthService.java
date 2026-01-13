package io.fayupable.jwtbasic.service;

import io.fayupable.jwtbasic.request.LoginRequest;
import io.fayupable.jwtbasic.request.RegisterRequest;
import io.fayupable.jwtbasic.response.AuthResponse;
import io.fayupable.jwtbasic.response.MessageResponse;
import io.fayupable.jwtbasic.entity.Role;
import io.fayupable.jwtbasic.entity.User;
import io.fayupable.jwtbasic.enums.RoleName;
import io.fayupable.jwtbasic.enums.UserStatus;
import io.fayupable.jwtbasic.repository.RoleRepository;
import io.fayupable.jwtbasic.repository.UserRepository;
import io.fayupable.jwtbasic.security.jwt.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Authentication Service
 * <p>
 * Handles user registration and login operations.
 * <p>
 * Main responsibilities:
 * - Register new users
 * - Authenticate users (login)
 * - Generate JWT tokens
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService implements IAuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    /**
     * Register a new user
     * <p>
     * Flow:
     * 1. Validate email is not taken
     * 2. Hash password
     * 3. Create user with default role
     * 4. Save to database
     *
     * @param request Registration details (email, password, username)
     * @return Success message
     */
    @Override
    @Transactional
    public MessageResponse register(RegisterRequest request) {
        log.info("Registration attempt for email: {}", request.getEmail());

        // Validate email is available
        validateEmailNotExists(request.getEmail());

        // Get default user role
        Role userRole = getDefaultUserRole();

        // Create and save user
        User user = createNewUser(request, userRole);
        userRepository.save(user);

        log.info("User registered successfully: {}", request.getEmail());
        return new MessageResponse("User registered successfully");
    }

    /**
     * Authenticate user and generate JWT token
     * <p>
     * Flow:
     * 1. Authenticate credentials
     * 2. Generate JWT token
     * 3. Extract user info
     * 4. Return token and user details
     *
     * @param request Login credentials (email, password)
     * @return JWT token and user information
     */
    @Override
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        // Authenticate user credentials
        Authentication authentication = authenticateUser(request);

        // Generate JWT token
        String token = jwtUtils.generateTokenForUser(authentication);

        // Extract user details from authentication
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        assert userDetails != null;
        List<String> roles = extractRoles(userDetails);

        log.info("Login successful for email: {}", request.getEmail());

        // Build and return response
        return buildAuthResponse(token, userDetails.getUsername(), roles);
    }


    /**
     * Validate that email is not already registered
     *
     * @param email Email to check
     * @throws RuntimeException if email already exists
     */
    private void validateEmailNotExists(String email) {
        if (userRepository.existsByEmail(email)) {
            log.error("Registration failed: Email already exists: {}", email);
            throw new RuntimeException("Email is already in use");
        }
    }

    /**
     * Get default user role from database
     *
     * @return ROLE_USER
     * @throws RuntimeException if role not found (database not initialized)
     */
    private Role getDefaultUserRole() {
        return roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new RuntimeException(
                        "Role USER not found. Please run database initialization."
                ));
    }

    /**
     * Create new user entity from registration request
     *
     * @param request Registration details
     * @param role    User role to assign
     * @return User entity ready to save
     */
    private User createNewUser(RegisterRequest request, Role role) {
        // Hash password with BCrypt
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        // Build user entity
        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(hashedPassword)
                .username(determineUsername(request))
                .status(UserStatus.ACTIVE)  // In production: PENDING_APPROVAL
                .build();

        // Assign role
        user.addRole(role);

        return user;
    }

    /**
     * Determine username from request
     * If username not provided, use email as username
     *
     * @param request Registration request
     * @return Username to use
     */
    private String determineUsername(RegisterRequest request) {
        return request.getUsername() != null
                ? request.getUsername()
                : request.getEmail();
    }

    /**
     * Authenticate user credentials using Spring Security
     *
     * @param request Login credentials
     * @return Authentication object if successful
     * @throws org.springframework.security.core.AuthenticationException if credentials invalid
     */
    private Authentication authenticateUser(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // Set authentication in security context
        SecurityContextHolder.getContext().setAuthentication(authentication);

        return authentication;
    }

    /**
     * Extract role names from user authorities
     *
     * @param userDetails User details from authentication
     * @return List of role names
     */
    private List<String> extractRoles(UserDetails userDetails) {
        return userDetails.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
    }

    /**
     * Build authentication response with token and user info
     *
     * @param token JWT token
     * @param email User email
     * @param roles User roles
     * @return Complete auth response
     */
    private AuthResponse buildAuthResponse(String token, String email, List<String> roles) {
        return AuthResponse.builder()
                .token(token)
                .email(email)
                .roles(roles)
                .build();
    }
}
