package io.fayupable.jwtbasic.init;

import io.fayupable.jwtbasic.entity.Role;
import io.fayupable.jwtbasic.entity.User;
import io.fayupable.jwtbasic.enums.RoleName;
import io.fayupable.jwtbasic.enums.UserStatus;
import io.fayupable.jwtbasic.repository.RoleRepository;
import io.fayupable.jwtbasic.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Database Initializer
 * <p>
 * This component runs on application startup and initializes the database with:
 * - Default roles (ROLE_USER, ROLE_ADMIN)
 * - Demo users for testing
 * <p>
 * Runs only once when database is empty.
 * Safe to run multiple times - checks if data already exists.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Runs on application startup
     * <p>
     * Order of initialization:
     * 1. Create roles (if not exist)
     * 2. Create demo users (if not exist)
     */
    @Override
    public void run(String... args) {
        log.info("Starting database initialization...");

        initializeRoles();
        initializeDemoUsers();

        log.info("Database initialization completed!");
    }

    /**
     * Initialize default roles
     * <p>
     * Creates:
     * - ROLE_USER: Default role for all users
     * - ROLE_ADMIN: Admin role with elevated permissions
     * <p>
     * Skips if roles already exist.
     */
    private void initializeRoles() {
        log.info("Checking if roles need to be initialized...");

        // Create ROLE_USER if not exists
        if (!roleRepository.existsByName(RoleName.ROLE_USER)) {
            Role userRole = Role.builder()
                    .name(RoleName.ROLE_USER)
                    .build();
            roleRepository.save(userRole);
            log.info("Created role: ROLE_USER");
        } else {
            log.info("ROLE_USER already exists");
        }

        // Create ROLE_ADMIN if not exists
        if (!roleRepository.existsByName(RoleName.ROLE_ADMIN)) {
            Role adminRole = Role.builder()
                    .name(RoleName.ROLE_ADMIN)
                    .build();
            roleRepository.save(adminRole);
            log.info("Created role: ROLE_ADMIN");
        } else {
            log.info("ROLE_ADMIN already exists");
        }
    }

    /**
     * Initialize demo users for testing
     * <p>
     * Creates two test users:
     * 1. Regular user (user@demo.com / password: user123)
     * 2. Admin user (admin@demo.com / password: admin123)
     * <p>
     * Skips if users already exist.
     * <p>
     * IMPORTANT: In production, remove this or use environment-specific profiles!
     */
    private void initializeDemoUsers() {
        log.info("Checking if demo users need to be created...");

        // Get roles from database
        Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("ROLE_USER not found"));

        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                .orElseThrow(() -> new RuntimeException("ROLE_ADMIN not found"));

        // Create regular user if not exists
        createDemoUser(
                "user@demo.com",
                "user123",
                "Demo User",
                userRole
        );

        // Create admin user if not exists
        createDemoUser(
                "admin@demo.com",
                "admin123",
                "Admin User",
                userRole,  // Admin also has ROLE_USER
                adminRole  // Plus ROLE_ADMIN
        );
    }

    /**
     * Helper method to create a demo user
     *
     * @param email    User email
     * @param password Plain text password (will be hashed)
     * @param username Display username
     * @param roles    Roles to assign to user
     */
    private void createDemoUser(String email, String password, String username, Role... roles) {
        // Check if user already exists
        if (userRepository.existsByEmail(email)) {
            log.info("User {} already exists", email);
            return;
        }

        // Hash password with BCrypt
        String hashedPassword = passwordEncoder.encode(password);

        // Create user entity
        User user = User.builder()
                .email(email)
                .passwordHash(hashedPassword)
                .username(username)
                .status(UserStatus.ACTIVE)
                .build();

        // Add roles to user
        for (Role role : roles) {
            user.addRole(role);
        }

        // Save to database
        userRepository.save(user);

        log.info("Created demo user: {} (password: {})", email, password);
    }
}