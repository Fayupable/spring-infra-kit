package io.fayupable.jwtbasic.repository;

import io.fayupable.jwtbasic.entity.Role;
import io.fayupable.jwtbasic.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Role Repository
 * <p>
 * Handles database operations for Role entity.
 * We extend JpaRepository to get basic CRUD operations for free.
 * <p>
 * Why JpaRepository<Role, UUID>?
 * - Role: The entity type we're working with
 * - UUID: The type of the primary key
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    /**
     * Find role by name
     * <p>
     * This method is used during:
     * - User registration (to assign default ROLE_USER)
     * - Role initialization (to check if roles exist)
     *
     * @param name The role name (e.g., ROLE_USER, ROLE_ADMIN)
     * @return Optional containing the role if found, empty otherwise
     * <p>
     * Spring Data JPA automatically generates the SQL:
     * SELECT * FROM roles WHERE name = ?
     */
    Optional<Role> findByName(RoleName name);

    /**
     * Check if role exists by name
     * <p>
     * Useful for initialization scripts to avoid duplicate inserts.
     *
     * @param name The role name to check
     * @return true if role exists, false otherwise
     * <p>
     * Spring Data JPA generates:
     * SELECT COUNT(*) > 0 FROM roles WHERE name = ?
     */
    boolean existsByName(RoleName name);
}