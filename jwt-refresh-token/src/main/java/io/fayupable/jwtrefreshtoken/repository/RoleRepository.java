package io.fayupable.jwtrefreshtoken.repository;

import io.fayupable.jwtrefreshtoken.entity.Role;
import io.fayupable.jwtrefreshtoken.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Role Repository
 * <p>
 * Data access layer for Role entity.
 * Provides methods to query roles from database.
 * <p>
 * Spring Data JPA automatically implements these methods.
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    /**
     * Find role by name
     * <p>
     * Used during:
     * - User registration (assign ROLE_USER)
     * - Role initialization (create default roles)
     * - Authorization checks
     *
     * @param name Role name enum
     * @return Optional containing role if found, empty otherwise
     * <p>
     * Example:
     * Optional<Role> userRole = roleRepository.findByRoleName(RoleName.ROLE_USER);
     */
    Optional<Role> findByRoleName(RoleName name);

    /**
     * Check if role exists by name
     * <p>
     * Used during initialization to avoid duplicate role creation.
     *
     * @param name Role name enum
     * @return true if role exists, false otherwise
     * <p>
     * Example:
     * if (!roleRepository.existsByRoleName(RoleName.ROLE_USER)) {
     * // Create role
     * }
     */
    boolean existsByRoleName(RoleName name);
}