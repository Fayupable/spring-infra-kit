package io.fayupable.specification.repository;

import io.fayupable.specification.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Customer Repository
 * <p>
 * Basic CRUD operations for Customer entity.
 * No specifications needed (focus is on Order entity).
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    /**
     * Find customer by email
     *
     * @param email Customer email
     * @return Optional customer
     */
    Optional<Customer> findByEmail(String email);

    /**
     * Check if customer exists by email
     *
     * @param email Customer email
     * @return true if exists
     */
    boolean existsByEmail(String email);
}