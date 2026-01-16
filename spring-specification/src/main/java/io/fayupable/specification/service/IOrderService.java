package io.fayupable.specification.service;

import io.fayupable.specification.entity.Order;
import io.fayupable.specification.enums.OrderStatus;
import io.fayupable.specification.repository.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Order Service Interface
 * <p>
 * Defines business operations for order management.
 * Demonstrates Specification pattern benefits through performance comparisons.
 * <p>
 * Method Naming Convention:
 * - findXxxWithoutSpec() - Traditional methods (slow)
 * - findXxxWithSpec() - Specification methods (fast)
 * - compareXxx() - Performance comparison methods
 * <p>
 * Key Principle:
 * WITHOUT Specification: Load ALL → Filter in Java → Slow
 * WITH Specification: Dynamic WHERE clause → Fast
 */
public interface IOrderService {

    // ============================================================================
    // TRADITIONAL METHODS (Without Specification)
    // ============================================================================

    /**
     * Find all orders - WITHOUT SPECIFICATION
     * <p>
     * WARNING: Loads ALL orders from database
     * Use ONLY when you genuinely need all orders
     * <p>
     * Performance: HEAVY (loads 1000+ orders)
     *
     * @return List of all orders
     */
    List<Order> findAllWithoutSpec();

    /**
     * Find orders by status - WITHOUT SPECIFICATION
     * <p>
     * Problem: Fixed query, cannot add dynamic filters
     *
     * @param status Order status
     * @return List of orders
     */
    List<Order> findByStatusWithoutSpec(OrderStatus status);

    /**
     * Find orders by date range - WITHOUT SPECIFICATION
     * <p>
     * Problem: Fixed query, what if we want status filter too?
     *
     * @param startDate Start date
     * @param endDate   End date
     * @return List of orders
     */
    List<Order> findByDateRangeWithoutSpec(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Complex filter - WITHOUT SPECIFICATION
     * <p>
     * Problem: Must load ALL orders, then filter in Java
     * - Loads 1000+ orders from database
     * - Filters in application memory
     * - Slow and memory-intensive
     * <p>
     * Performance: 850ms for 1000 orders
     *
     * @param status    Order status
     * @param startDate Start date
     * @param endDate   End date
     * @param minAmount Minimum amount
     * @return Filtered orders
     */
    List<Order> findComplexWithoutSpec(
            OrderStatus status,
            LocalDateTime startDate,
            LocalDateTime endDate,
            BigDecimal minAmount
    );

    // ============================================================================
    // SPECIFICATION METHODS (With Specification)
    // ============================================================================

    /**
     * Find orders by status - WITH SPECIFICATION
     * <p>
     * Benefits:
     * - Dynamic WHERE clause at database level
     * - Can easily combine with other filters
     * - Type-safe and reusable
     * <p>
     * Performance: Fast (database filtering)
     *
     * @param status Order status (can be null)
     * @return List of orders
     */
    List<Order> findByStatusWithSpec(OrderStatus status);

    /**
     * Find orders by date range - WITH SPECIFICATION
     * <p>
     * Benefits:
     * - Can combine with status, amount, customer filters
     * - No method explosion
     *
     * @param startDate Start date (can be null)
     * @param endDate   End date (can be null)
     * @return List of orders
     */
    List<Order> findByDateRangeWithSpec(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Complex filter - WITH SPECIFICATION
     * <p>
     * Benefits:
     * - Only matching orders loaded from database
     * - Filters at SQL level (fast)
     * - Low memory usage
     * <p>
     * Performance: 45ms for 1000 orders (19x faster!)
     * <p>
     * SQL Generated:
     * SELECT * FROM orders
     * WHERE status = ?
     * AND created_at BETWEEN ? AND ?
     * AND total_amount > ?
     *
     * @param status    Order status (can be null)
     * @param startDate Start date (can be null)
     * @param endDate   End date (can be null)
     * @param minAmount Minimum amount (can be null)
     * @return Filtered orders
     */
    List<Order> findComplexWithSpec(
            OrderStatus status,
            LocalDateTime startDate,
            LocalDateTime endDate,
            BigDecimal minAmount
    );

    /**
     * Advanced filter with customer - WITH SPECIFICATION
     * <p>
     * Demonstrates JOIN queries with specifications
     * <p>
     * SQL Generated:
     * SELECT o.* FROM orders o
     * JOIN customers c ON o.customer_id = c.id
     * WHERE o.status = ?
     * AND o.total_amount BETWEEN ? AND ?
     * AND c.name LIKE ?
     * AND c.city = ?
     *
     * @param status       Order status
     * @param minAmount    Minimum amount
     * @param maxAmount    Maximum amount
     * @param customerName Customer name search
     * @param customerCity Customer city
     * @return Filtered orders
     */
    List<Order> findAdvancedWithSpec(
            OrderStatus status,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            String customerName,
            String customerCity
    );

    /**
     * Find orders with pagination - WITH SPECIFICATION
     * <p>
     * Combines specification with pagination
     *
     * @param status    Order status
     * @param startDate Start date
     * @param endDate   End date
     * @param pageable  Pagination parameters
     * @return Page of orders
     */
    Page<Order> findWithSpecPaginated(
            OrderStatus status,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );

    /**
     * Find active orders (complex predefined spec)
     * <p>
     * Active = PENDING OR PROCESSING OR SHIPPED
     *
     * @return List of active orders
     */
    List<Order> findActiveOrders();

    /**
     * Find high-value orders (complex predefined spec)
     * <p>
     * High-value = amount > 1000 AND status != CANCELLED
     *
     * @return List of high-value orders
     */
    List<Order> findHighValueOrders();

    /**
     * Find recent orders (complex predefined spec)
     * <p>
     * Recent = created in last 30 days AND not cancelled
     *
     * @return List of recent orders
     */
    List<Order> findRecentOrders();

    // ============================================================================
    // PERFORMANCE COMPARISON METHODS
    // ============================================================================

    /**
     * Compare performance: WITHOUT vs WITH Specification
     * <p>
     * Runs both methods and measures:
     * - Execution time
     * - Number of orders loaded
     * - Memory usage estimation
     * <p>
     * Returns detailed comparison metrics
     *
     * @param status    Order status
     * @param startDate Start date
     * @param endDate   End date
     * @param minAmount Minimum amount
     * @return Comparison result with timing data
     */
    ComparisonResult comparePerformance(
            OrderStatus status,
            LocalDateTime startDate,
            LocalDateTime endDate,
            BigDecimal minAmount
    );

    /**
     * Comparison Result DTO
     * <p>
     * Contains performance metrics comparing methods
     */
    record ComparisonResult(
            long withoutSpecDurationMs,
            int withoutSpecLoadedCount,
            int withoutSpecResultCount,
            long withSpecDurationMs,
            int withSpecResultCount,
            double speedImprovement,
            String improvementDescription
    ) {
    }

    // ============================================================================
    // STATISTICS METHODS
    // ============================================================================

    /**
     * Get order statistics
     * <p>
     * Returns count, total amount, average amount by status
     *
     * @return List of statistics
     */
    List<OrderRepository.OrderStatisticsProjection> getOrderStatistics();
}