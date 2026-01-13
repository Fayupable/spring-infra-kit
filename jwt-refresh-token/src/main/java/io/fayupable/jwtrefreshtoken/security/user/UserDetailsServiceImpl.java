package io.fayupable.jwtrefreshtoken.security.user;

import io.fayupable.jwtrefreshtoken.entity.User;
import io.fayupable.jwtrefreshtoken.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UserDetailsService Implementation
 * <p>
 * This service is Spring Security's bridge to your user database.
 * <p>
 * When a user tries to login:
 * 1. Spring Security calls loadUserByUsername(input)
 * 2. We fetch user from database (checking BOTH username and email)
 * 3. We convert User entity to UserDetails
 * 4. Spring Security compares passwords
 * 5. Spring Security checks account status (enabled, locked, etc.)
 * 6. If everything OK, user is authenticated
 * <p>
 * This service is also called during:
 * - JWT token validation (to reload user data)
 * - Authorization checks (to get current user's roles)
 * - Any Spring Security operation requiring user info
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Load User by Username OR Email
     * <p>
     * This is the core method that Spring Security calls to load user data.
     * UPDATED: Now supports "Smart Login" (Username OR Email).
     * <p>
     * Flow:
     * 1. Receive input string (could be "fayupable" or "test@mail.com")
     * 2. Search database using findByUsernameOrEmail
     * 3. If found in EITHER column, return user
     * 4. If not found in ANY column, throw exception
     * <p>
     * Why Transactional?
     * - User has EAGER roles relationship
     * - We want to ensure roles are loaded in same transaction
     * - Prevents LazyInitializationException
     *
     * @param usernameOrEmail Input from login form (Username or Email)
     * @return UserDetails object for Spring Security
     * @throws UsernameNotFoundException if user not found
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(@NonNull String usernameOrEmail) throws UsernameNotFoundException {
        log.debug("Loading user by identifier: {}", usernameOrEmail);

        // We pass the same input string to both parameters.
        // Logic: Is this string a username? OR Is this string an email?
        User user = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> {
                    log.error("User not found with identifier: {}", usernameOrEmail);
                    return new UsernameNotFoundException(
                            "User not found with username or email: " + usernameOrEmail
                    );
                });

        log.debug("User found: {} (Username: {})", user.getEmail(), user.getUsername());

        // Convert User entity to UserDetails
        return UserDetailsImpl.build(user);
    }
}