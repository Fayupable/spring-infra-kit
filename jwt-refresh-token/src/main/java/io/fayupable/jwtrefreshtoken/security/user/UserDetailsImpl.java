package io.fayupable.jwtrefreshtoken.security.user;

import io.fayupable.jwtrefreshtoken.entity.User;
import io.fayupable.jwtrefreshtoken.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * UserDetails Implementation
 * <p>
 * Spring Security's representation of a user.
 * Wraps our User entity and provides security-related information.
 * <p>
 * Spring Security uses this to:
 * - Check if user can login (isEnabled, isAccountNonLocked)
 * - Get user credentials (username, password)
 * - Get user authorities/roles (getAuthorities)
 * <p>
 * This class is returned by UserDetailsService and used throughout
 * the authentication and authorization process.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserDetailsImpl implements UserDetails {

    /**
     * User's unique identifier
     * Stored for easy access during authentication
     */
    private UUID id;

    /**
     * User's email (used as username)
     * This is what user enters in login form
     */
    private String email;

    /**
     * User's hashed password
     * BCrypt hash, never plain text!
     */
    private String password;

    /**
     * User's current account status
     * Determines if user can login and access system
     */
    private UserStatus status;

    /**
     * User's roles/authorities
     * Used for authorization checks (@PreAuthorize, etc.)
     */
    private Collection<GrantedAuthority> authorities;

    /**
     * Get username for Spring Security
     * <p>
     * We use email as username in this system.
     * User logs in with email, not a separate username.
     *
     * @return User's email
     */
    @Override
    public String getUsername() {
        return email;
    }

    /**
     * Get password for Spring Security
     * <p>
     * Returns BCrypt hashed password.
     * Spring Security will compare this with login password hash.
     *
     * @return Hashed password
     */
    @Override
    public String getPassword() {
        return password;
    }

    /**
     * Get user's authorities (roles)
     * <p>
     * Spring Security uses this for authorization:
     * - @PreAuthorize("hasRole('ADMIN')")
     * - @Secured("ROLE_USER")
     * - SecurityConfig authorization rules
     *
     * @return Collection of granted authorities
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    /**
     * Check if account is enabled
     * <p>
     * Account is enabled ONLY if status is ACTIVE.
     * <p>
     * PENDING_APPROVAL users cannot login until email verified.
     * This prevents unverified accounts from accessing the system.
     *
     * @return true if status is ACTIVE, false otherwise
     */
    @Override
    public boolean isEnabled() {
        return this.status == UserStatus.ACTIVE;
    }

    /**
     * Check if account is not locked
     * <p>
     * Account is locked if user is BANNED or SUSPENDED.
     * <p>
     * Locked accounts cannot login even with correct password.
     * This is different from disabled - it's a security measure.
     * <p>
     * Use cases:
     * - BANNED: Permanent block (terms violation)
     * - SUSPENDED: Temporary block (suspicious activity)
     *
     * @return true if not BANNED or SUSPENDED, false otherwise
     */
    @Override
    public boolean isAccountNonLocked() {
        return this.status != UserStatus.BANNED
                && this.status != UserStatus.SUSPENDED;
    }

    /**
     * Check if account is not expired
     * <p>
     * We don't use account expiration in this demo.
     * All accounts are considered non-expired.
     * <p>
     * In production, you might:
     * - Expire inactive accounts after 1 year
     * - Expire trial accounts after 30 days
     * - Expire temporary guest accounts
     *
     * @return true (accounts don't expire in this demo)
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Check if credentials are not expired
     * <p>
     * We don't force password changes in this demo.
     * All passwords are considered non-expired.
     * <p>
     * In production, you might:
     * - Force password change every 90 days
     * - Expire passwords after security breach
     * - Expire default/temporary passwords
     *
     * @return true (passwords don't expire in this demo)
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Build UserDetails from User Entity
     * <p>
     * Static factory method that converts our User entity
     * into Spring Security's UserDetails format.
     * <p>
     * This is called by UserDetailsService when loading user
     * from database during authentication.
     * <p>
     * Conversion:
     * - User.userId → UserDetailsImpl.id
     * - User.email → UserDetailsImpl.email
     * - User.passwordHash → UserDetailsImpl.password
     * - User.status → UserDetailsImpl.status
     * - User.roles → UserDetailsImpl.authorities (converted)
     *
     * @param user User entity from database
     * @return UserDetails implementation ready for Spring Security
     */
    public static UserDetailsImpl build(User user) {
        // Convert Role entities to GrantedAuthority objects
        // Spring Security uses GrantedAuthority interface for roles
        // We convert RoleName enum to string (e.g., ROLE_USER, ROLE_ADMIN)
        Collection<GrantedAuthority> authorities = user.getRoles()
                .stream()
                .map(role -> new SimpleGrantedAuthority(role.getRoleName().name()))
                .collect(Collectors.toList());

        return new UserDetailsImpl(
                user.getUserId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getUserStatus(),
                authorities
        );
    }
}