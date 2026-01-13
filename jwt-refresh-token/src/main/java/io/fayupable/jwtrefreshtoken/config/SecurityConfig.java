package io.fayupable.jwtrefreshtoken.config;

import io.fayupable.jwtrefreshtoken.security.jwt.JwtAuthenticationFilter;
import io.fayupable.jwtrefreshtoken.security.jwt.JwtAuthEntryPoint;
import io.fayupable.jwtrefreshtoken.security.user.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security Configuration
 * <p>
 * Configures security for JWT-based authentication with refresh tokens.
 * <p>
 * Key Features:
 * - Stateless session management
 * - JWT token authentication
 * - CORS configuration
 * - Public and protected endpoints
 * - BCrypt password encoding
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // Enables @PreAuthorize, @Secured annotations
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final JwtAuthEntryPoint authEntryPoint;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${app.defaults.base-path:/api/auth}")
    private String basePath;

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    @Value("${cors.allowed-methods}")
    private String allowedMethods;

    @Value("${cors.allowed-headers}")
    private String allowedHeaders;

    @Value("${cors.allow-credentials}")
    private boolean allowCredentials;

    /**
     * Public endpoints that don't require authentication
     */
    private List<String> getPublicUrls() {
        return List.of(
                basePath + "/register",      // User registration
                basePath + "/login",          // User login
                basePath + "/refresh",         // Token refresh
                "/actuator/health",
                "/actuator/health/**",
                "/error",
                "/error/**"
        );
    }

    /**
     * Password Encoder Bean
     * <p>
     * BCrypt is industry standard for password hashing.
     * Automatically handles salting and is computationally expensive.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Authentication Manager Bean
     * <p>
     * Used by login endpoint to authenticate users.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * DAO Authentication Provider
     * <p>
     * Connects UserDetailsService with PasswordEncoder.
     * Handles username/password authentication.
     * <p>
     * Spring Boot 4.0+ requires UserDetailsService in constructor.
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Security Filter Chain
     * <p>
     * Main security configuration:
     * - Disables CSRF (using JWT, not cookies)
     * - Enables CORS
     * - Configures public/protected endpoints
     * - Adds JWT authentication filter
     * - Sets session to stateless
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF (JWT is stateless)
                .csrf(AbstractHttpConfigurer::disable)

                // Configure CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Configure exception handling
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authEntryPoint)
                )

                // Stateless session management (no server-side sessions)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Configure authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Allow OPTIONS requests (CORS preflight)
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**")
                        .permitAll()

                        // Public endpoints
                        .requestMatchers(getPublicUrls().toArray(String[]::new))
                        .permitAll()

                        // All other endpoints require authentication
                        .anyRequest()
                        .authenticated()
                );

        // Set authentication provider
        http.authenticationProvider(authenticationProvider());

        // Add JWT filter before UsernamePasswordAuthenticationFilter
        http.addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
        );

        return http.build();
    }

    /**
     * CORS Configuration
     * <p>
     * Allows cross-origin requests from specified origins.
     * Required for frontend applications on different domains/ports.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Parse and set allowed origins
        configuration.setAllowedOriginPatterns(
                Arrays.stream(allowedOrigins.split(","))
                        .map(String::trim)
                        .toList()
        );

        // Parse and set allowed methods
        configuration.setAllowedMethods(
                Arrays.stream(allowedMethods.split(","))
                        .map(String::trim)
                        .toList()
        );

        // Parse and set allowed headers
        configuration.setAllowedHeaders(
                Arrays.stream(allowedHeaders.split(","))
                        .map(String::trim)
                        .toList()
        );

        // Allow credentials (cookies, authorization headers)
        configuration.setAllowCredentials(allowCredentials);

        // Cache preflight response for 1 hour
        configuration.setMaxAge(3600L);

        // Apply CORS configuration to all paths
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}