package io.fayupable.jwtbasic.security.user;

import io.fayupable.jwtbasic.entity.User;
import io.fayupable.jwtbasic.enums.UserStatus;
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
 *
 * This class is Spring Security's representation of a user.
 * It wraps our User entity and provides security-related information.
 *
 * Spring Security uses this to:
 * - Check if user can login (isEnabled, isAccountNonLocked)
 * - Get user credentials (username, password)
 * - Get user authorities/roles (getAuthorities)
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserDetailsImpl implements UserDetails {

    /**
     * User's unique identifier
     */
    private UUID id;

    /**
     * User's email (used as username for authentication)
     */
    private String email;

    /**
     * User's hashed password
     * Never store plain text passwords!
     */
    private String password;

    /**
     * User's current status
     * Used to determine if account is enabled or locked
     */
    private UserStatus status;

    /**
     * User's roles/authorities
     * Example: [ROLE_USER, ROLE_ADMIN]
     */
    private Collection<GrantedAuthority> authorities;

    /**
     * Get username for Spring Security
     *
     * We use email as username in this application.
     *
     * @return User's email
     */
    @Override
    public String getUsername() {
        return email;
    }

    /**
     * Get password for Spring Security
     *
     * Returns the BCrypt hashed password.
     * Spring Security will compare this with the hashed login password.
     *
     * @return Hashed password
     */
    @Override
    public String getPassword() {
        return password;
    }

    /**
     * Get user's authorities (roles)
     *
     * Spring Security uses this for authorization checks.
     * Example: @PreAuthorize("hasRole('ADMIN')")
     *
     * @return Collection of granted authorities
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    /**
     * Check if account is enabled
     *
     * An account is enabled if status is ACTIVE.
     * PENDING_APPROVAL users cannot login until verified.
     *
     * @return true if account can login, false otherwise
     */
    @Override
    public boolean isEnabled() {
        return this.status == UserStatus.ACTIVE;
    }

    /**
     * Check if account is not locked
     *
     * Account is locked if user is BANNED or SUSPENDED.
     * Locked accounts cannot login even if they have correct credentials.
     *
     * @return true if account is not locked, false otherwise
     */
    @Override
    public boolean isAccountNonLocked() {
        return this.status != UserStatus.BANNED
                && this.status != UserStatus.SUSPENDED;
    }

    /**
     * Check if account is not expired
     *
     * We don't use account expiration in this demo,
     * so we always return true.
     *
     * In production, you might expire accounts after inactivity.
     *
     * @return true (accounts don't expire in this demo)
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Check if credentials are not expired
     *
     * We don't force password changes in this demo,
     * so we always return true.
     *
     * In production, you might require password changes every 90 days.
     *
     * @return true (passwords don't expire in this demo)
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Build UserDetails from User entity
     *
     * This static factory method converts our User entity
     * into Spring Security's UserDetails format.
     *
     * It extracts:
     * - User ID
     * - Email
     * - Password hash
     * - Status
     * - Roles (converted to GrantedAuthority)
     *
     * @param user User entity from database
     * @return UserDetails implementation
     */
    public static UserDetailsImpl build(User user) {
        // Convert Role entities to GrantedAuthority objects
        // Spring Security uses GrantedAuthority for authorization
        Collection<GrantedAuthority> authorities = user.getRoles()
                .stream()
                .map(role -> new SimpleGrantedAuthority(role.getName().name()))
                .collect(Collectors.toList());

        return new UserDetailsImpl(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getStatus(),
                authorities
        );
    }
}