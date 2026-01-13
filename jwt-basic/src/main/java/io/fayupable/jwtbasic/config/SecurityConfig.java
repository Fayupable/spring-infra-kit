package io.fayupable.jwtbasic.config;

import io.fayupable.jwtbasic.security.jwt.JwtAuthenticationFilter;
import io.fayupable.jwtbasic.security.jwt.JwtAuthEntryPoint;
import io.fayupable.jwtbasic.security.user.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security Configuration
 * <p>
 * This is the central configuration for security in our application.
 * <p>
 * Key configurations:
 * - Password encoding (BCrypt)
 * - Authentication provider (how to verify credentials)
 * - Security filter chain (which endpoints are protected)
 * - JWT filter integration
 * - Stateless session management
 * <p>
 * Flow:
 * 1. Request comes in
 * 2. JwtAuthenticationFilter extracts and validates token
 * 3. If valid, user is authenticated
 * 4. SecurityFilterChain checks if user has access to endpoint
 * 5. If authorized, request proceeds; otherwise, 401/403 returned
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // Enables @PreAuthorize, @Secured annotations
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final JwtAuthEntryPoint authEntryPoint;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Password Encoder Bean
     * <p>
     * BCrypt is a one-way hashing function designed for passwords.
     * <p>
     * Why BCrypt?
     * - Automatically handles salting (random data added to passwords)
     * - Computationally expensive (slow) - protects against brute force
     * - Industry standard for password hashing
     * <p>
     * When user registers:
     * - Plain password: "mypassword123"
     * - BCrypt hash: "$2a$10$N9qo8uLOickgx2ZMRZoMye..."
     * <p>
     * When user logs in:
     * - BCrypt compares plain password with stored hash
     * - If match, authentication succeeds
     *
     * @return BCryptPasswordEncoder instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Authentication Manager Bean
     * <p>
     * This is Spring Security's main component for authentication.
     * <p>
     * Used by:
     * - Login endpoint (to authenticate username/password)
     * - Any custom authentication logic
     * <p>
     * We get it from AuthenticationConfiguration which Spring provides.
     *
     * @param authConfig Spring's authentication configuration
     * @return AuthenticationManager instance
     * @throws Exception if configuration fails
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig)
            throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * DAO Authentication Provider
     * <p>
     * This provider tells Spring Security:
     * - How to load users (UserDetailsService)
     * - How to check passwords (PasswordEncoder)
     * <p>
     * During login:
     * 1. User submits email + password
     * 2. DaoAuthenticationProvider loads user via UserDetailsService
     * 3. Compares submitted password with stored hash using PasswordEncoder
     * 4. If match, creates Authentication object
     * 5. User is authenticated
     *
     * @return Configured DaoAuthenticationProvider
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        // Spring Boot 4.0+ requires UserDetailsService in constructor
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Security Filter Chain
     * <p>
     * This is the main security configuration that defines:
     * - Which endpoints are public (no authentication required)
     * - Which endpoints are protected (authentication required)
     * - How to handle unauthorized access
     * - Session management strategy
     * - Filter order
     *
     * @param http HttpSecurity builder
     * @return Configured SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF protection
                // Why? We're using JWT tokens, not cookies
                // CSRF protection is needed for cookie-based auth, not token-based
                .csrf(AbstractHttpConfigurer::disable)

                // Configure exception handling
                // When authentication fails (no token, invalid token, etc.)
                // Use JwtAuthEntryPoint to return JSON error response
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authEntryPoint)
                )

                // Configure session management
                // STATELESS: Don't create HTTP sessions
                // Why? JWT is stateless - all info is in the token
                // We don't need server-side sessions
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Configure authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints - anyone can access
                        .requestMatchers(
                                "/api/auth/register",    // User registration
                                "/api/auth/login"        // User login
                        ).permitAll()

                        // Protected endpoints - authentication required
                        // Any other request requires authentication
                        .anyRequest().authenticated()
                )

                // Set authentication provider
                .authenticationProvider(authenticationProvider())

                // Add JWT filter before Spring's username/password filter
                // This ensures JWT validation happens first
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}