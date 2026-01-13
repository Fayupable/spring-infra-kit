package io.fayupable.jwtbasic.security.user;

import io.fayupable.jwtbasic.entity.User;
import io.fayupable.jwtbasic.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * UserDetailsService Implementation
 * <p>
 * This service is used by Spring Security to load user data from database
 * during authentication.
 * <p>
 * When a user tries to login:
 * 1. Spring Security calls loadUserByUsername(email)
 * 2. We fetch the user from database
 * 3. We convert User entity to UserDetails
 * 4. Spring Security compares passwords and checks account status
 * 5. If everything is OK, user is authenticated
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Load user by username (email in our case)
     * <p>
     * This method is called by Spring Security during:
     * - Login process
     * - JWT token validation
     * - Any authentication attempt
     * <p>
     * Flow:
     * 1. Search user in database by email
     * 2. If not found, throw UsernameNotFoundException
     * 3. If found, convert to UserDetails and return
     *
     * @param username User's email (we use email as username)
     * @return UserDetails object for authentication
     * @throws UsernameNotFoundException if user not found
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user by username: {}", username);

        // Find user by email
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> {
                    log.error("User not found with email: {}", username);
                    return new UsernameNotFoundException("User not found with email: " + username);
                });

        log.debug("User found: {} with status: {}", user.getEmail(), user.getStatus());

        // Convert User entity to UserDetails
        return UserDetailsImpl.build(user);
    }
}