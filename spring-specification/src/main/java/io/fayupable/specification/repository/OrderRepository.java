package io.fayupable.specification.repository;

import io.fayupable.specification.entity.Order;
import io.fayupable.specification.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Order Repository
 * <p>
 * Main repository for demonstrating Specification pattern.
 * <p>
 * Key Interface: JpaSpecificationExecutor<Order>
 * This interface provides specification methods:
 * - findAll(Specification<Order> spec)
 * - findOne(Specification<Order> spec)
 * - count(Specification<Order> spec)
 * - exists(Specification<Order> spec)
 * <p>
 * Comparison Methods:
 * 1. Traditional methods (without spec) - for performance comparison
 * 2. Specification methods (inherited from JpaSpecificationExecutor)
 * <p>
 * Performance Demo:
 * Traditional: Load all → Filter in Java → Slow
 * Specification: Dynamic WHERE → Fast
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    // ============================================================================
    // TRADITIONAL METHODS (Without Specification)
    // Used for performance comparison in demo
    // ============================================================================

    /**
     * Find orders by status - TRADITIONAL METHOD
     * <p>
     * Problem: Fixed query, cannot combine with other filters dynamically
     * <p>
     * SQL: SELECT * FROM orders WHERE status = ?
     *
     * @param status Order status
     * @return List of orders
     */
    List<Order> findByStatus(OrderStatus status);

    /**
     * Find orders created between dates - TRADITIONAL METHOD
     * <p>
     * Problem: Fixed query, what if we want to add status filter?
     * <p>
     * SQL: SELECT * FROM orders WHERE created_at BETWEEN ? AND ?
     *
     * @param startDate Start date
     * @param endDate   End date
     * @return List of orders
     */
    List<Order> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find orders by status and amount - TRADITIONAL METHOD
     * <p>
     * Problem: Need separate method for each combination
     * What about: status + date + amount + city? → Explosion of methods!
     * <p>
     * SQL: SELECT * FROM orders WHERE status = ? AND total_amount > ?
     *
     * @param status    Order status
     * @param minAmount Minimum amount
     * @return List of orders
     */
    List<Order> findByStatusAndTotalAmountGreaterThan(OrderStatus status, BigDecimal minAmount);

    /**
     * Find orders by customer city - TRADITIONAL METHOD (with JOIN)
     * <p>
     * Problem: Fixed query, cannot add dynamic filters
     * <p>
     * SQL: SELECT o.* FROM orders o
     * JOIN customers c ON o.customer_id = c.id
     * WHERE c.city = ?
     *
     * @param city Customer city
     * @return List of orders
     */
    @Query("SELECT o FROM Order o JOIN o.customer c WHERE c.city = :city")
    List<Order> findByCustomerCity(@Param("city") String city);

    // ============================================================================
    // SPECIFICATION METHODS
    // Inherited from JpaSpecificationExecutor<Order>
    // ============================================================================

    /*
     * No need to define these - they come from JpaSpecificationExecutor:
     *
     * List<Order> findAll(Specification<Order> spec);
     * Page<Order> findAll(Specification<Order> spec, Pageable pageable);
     * long count(Specification<Order> spec);
     * boolean exists(Specification<Order> spec);
     * Optional<Order> findOne(Specification<Order> spec);
     *
     * Usage Example:
     * Specification<Order> spec = Specification
     *     .where(OrderSpecification.hasStatus(OrderStatus.COMPLETED))
     *     .and(OrderSpecification.createdBetween(start, end))
     *     .and(OrderSpecification.totalAmountGreaterThan(BigDecimal.valueOf(100)));
     *
     * List<Order> orders = orderRepository.findAll(spec);
     *
     * Benefits:
     * 1. Single method handles ANY combination of filters
     * 2. Filters composed at runtime
     * 3. Type-safe and compile-time checked
     * 4. Database-level filtering (not in-memory)
     * 5. No method explosion
     */

    // ============================================================================
    // STATISTICS METHODS
    // ============================================================================

    /**
     * Count orders by status
     * <p>
     * Used for dashboard statistics
     *
     * @param status Order status
     * @return Count of orders
     */
    long countByStatus(OrderStatus status);

    /**
     * Get total amount by status
     * <p>
     * Used for revenue analytics
     *
     * @param status Order status
     * @return Total amount
     */
    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.status = :status")
    BigDecimal getTotalAmountByStatus(@Param("status") OrderStatus status);

    /**
     * Get order statistics
     * <p>
     * Custom projection for statistics
     */
    @Query("SELECT o.status as status, " +
            "COUNT(o) as orderCount, " +
            "SUM(o.totalAmount) as totalAmount, " +
            "AVG(o.totalAmount) as averageAmount " +
            "FROM Order o " +
            "GROUP BY o.status")
    List<OrderStatisticsProjection> getOrderStatistics();

    /**
     * Order Statistics Projection
     * Used for aggregate queries
     */
    interface OrderStatisticsProjection {
        OrderStatus getStatus();

        Long getOrderCount();

        BigDecimal getTotalAmount();

        Double getAverageAmount();
    }
}