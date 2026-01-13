package io.fayupable.jwtrefreshtoken.init;

import io.fayupable.jwtrefreshtoken.entity.Role;
import io.fayupable.jwtrefreshtoken.entity.User;
import io.fayupable.jwtrefreshtoken.enums.RoleName;
import io.fayupable.jwtrefreshtoken.enums.UserStatus;
import io.fayupable.jwtrefreshtoken.repository.RoleRepository;
import io.fayupable.jwtrefreshtoken.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Database Initializer
 * <p>
 * This component runs on application startup and initializes the database with:
 * - Default roles (ROLE_USER, ROLE_ADMIN)
 * - Demo users for testing
 * <p>
 * Runs only once when database is empty (checks for existence).
 * Safe to run multiple times.
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
    @Transactional
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
     */
    private void initializeRoles() {
        log.info("Checking if roles need to be initialized...");

        createRoleIfNotExists(RoleName.ROLE_USER);
        createRoleIfNotExists(RoleName.ROLE_ADMIN);
    }

    /**
     * Helper to create a role if it doesn't exist
     */
    private void createRoleIfNotExists(RoleName roleName) {
        if (!roleRepository.existsByRoleName(roleName)) {
            Role role = Role.builder()
                    .roleName(roleName)
                    .build();
            roleRepository.save(role);
            log.info("Created role: {}", roleName);
        } else {
            log.info("{} already exists", roleName);
        }
    }

    /**
     * Initialize demo users for testing
     * <p>
     * Creates two test users:
     * 1. Regular user (user@demo.com / password: user123)
     * 2. Admin user (admin@demo.com / password: admin123)
     */
    private void initializeDemoUsers() {
        log.info("Checking if demo users need to be created...");

        // Fetch Roles from DB
        Role userRole = roleRepository.findByRoleName(RoleName.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("Error: ROLE_USER is not found."));

        Role adminRole = roleRepository.findByRoleName(RoleName.ROLE_ADMIN)
                .orElseThrow(() -> new RuntimeException("Error: ROLE_ADMIN is not found."));

        // Create Regular User
        createDemoUser(
                "user@demo.com",
                "user123",
                "Demo User",
                userRole
        );

        // Create Admin User (Has both USER and ADMIN roles)
        createDemoUser(
                "admin@demo.com",
                "admin123",
                "Admin User",
                userRole,
                adminRole
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

        // Create User Entity
        User user = User.builder()
                .email(email)
                .username(username)
                .passwordHash(passwordEncoder.encode(password))// Hash password
                .build();

        // Assign Roles
        user.setUserStatus(UserStatus.ACTIVE);
        Set<Role> roleSet = new HashSet<>(Arrays.asList(roles));
        user.setRoles(roleSet);

        // Save to Database
        userRepository.save(user);

        log.info("Created demo user: {} (password: {})", email, password);
    }
}