package io.fayupable.specification.repository.specification;

import io.fayupable.specification.entity.Customer;
import io.fayupable.specification.entity.Order;
import io.fayupable.specification.enums.OrderStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Order Specification
 * <p>
 * Contains all dynamic query specifications for Order entity.
 * Each method returns a Specification that can be combined with others.
 * <p>
 * Key Concept:
 * Specifications allow building dynamic WHERE clauses programmatically.
 * <p>
 * Benefits:
 * 1. Type-safe queries (compile-time checking)
 * 2. Reusable query components
 * 3. Dynamic query building based on runtime conditions
 * 4. Database-level filtering (not in-memory)
 * 5. Composable with AND/OR logic
 * <p>
 * Example Usage:
 * Specification<Order> spec = Specification
 * .where(hasStatus(OrderStatus.COMPLETED))
 * .and(createdBetween(start, end))
 * .and(totalAmountGreaterThan(100));
 * <p>
 * List<Order> orders = orderRepository.findAll(spec);
 * <p>
 * Generated SQL:
 * SELECT * FROM orders
 * WHERE status = 'COMPLETED'
 * AND created_at BETWEEN ? AND ?
 * AND total_amount > ?
 */
public class OrderSpecification {

    // ============================================================================
    // STATUS SPECIFICATIONS
    // ============================================================================

    /**
     * Filter by exact status
     * <p>
     * Usage: hasStatus(OrderStatus.COMPLETED)
     * SQL: WHERE status = 'COMPLETED'
     *
     * @param status Order status to filter
     * @return Specification for status match
     */
    public static Specification<Order> hasStatus(OrderStatus status) {
        return (root, query, criteriaBuilder) -> {
            if (status == null) {
                return criteriaBuilder.conjunction(); // Always true (no filter)
            }
            return criteriaBuilder.equal(root.get("status"), status);
        };
    }

    /**
     * Filter by multiple statuses (IN clause)
     * <p>
     * Usage: hasStatusIn(Arrays.asList(PENDING, PROCESSING))
     * SQL: WHERE status IN ('PENDING', 'PROCESSING')
     *
     * @param statuses List of statuses
     * @return Specification for status IN clause
     */
    public static Specification<Order> hasStatusIn(List<OrderStatus> statuses) {
        return (root, query, criteriaBuilder) -> {
            if (statuses == null || statuses.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return root.get("status").in(statuses);
        };
    }

    /**
     * Filter by status NOT equal
     * <p>
     * Usage: statusNotEqual(OrderStatus.CANCELLED)
     * SQL: WHERE status != 'CANCELLED'
     *
     * @param status Status to exclude
     * @return Specification for status not equal
     */
    public static Specification<Order> statusNotEqual(OrderStatus status) {
        return (root, query, criteriaBuilder) -> {
            if (status == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.notEqual(root.get("status"), status);
        };
    }

    // ============================================================================
    // DATE SPECIFICATIONS
    // ============================================================================

    /**
     * Filter orders created after date
     * <p>
     * Usage: createdAfter(LocalDateTime.of(2024, 1, 1, 0, 0))
     * SQL: WHERE created_at > '2024-01-01 00:00:00'
     *
     * @param date Minimum creation date
     * @return Specification for date greater than
     */
    public static Specification<Order> createdAfter(LocalDateTime date) {
        return (root, query, criteriaBuilder) -> {
            if (date == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThan(root.get("createdAt"), date);
        };
    }

    /**
     * Filter orders created before date
     * <p>
     * Usage: createdBefore(LocalDateTime.of(2024, 12, 31, 23, 59))
     * SQL: WHERE created_at < '2024-12-31 23:59:00'
     *
     * @param date Maximum creation date
     * @return Specification for date less than
     */
    public static Specification<Order> createdBefore(LocalDateTime date) {
        return (root, query, criteriaBuilder) -> {
            if (date == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThan(root.get("createdAt"), date);
        };
    }

    /**
     * Filter orders created between dates
     * <p>
     * Usage: createdBetween(start, end)
     * SQL: WHERE created_at BETWEEN '2024-01-01' AND '2024-12-31'
     *
     * @param startDate Start date
     * @param endDate   End date
     * @return Specification for date range
     */
    public static Specification<Order> createdBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return (root, query, criteriaBuilder) -> {
            if (startDate == null && endDate == null) {
                return criteriaBuilder.conjunction();
            }
            if (startDate == null) {
                return criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), endDate);
            }
            if (endDate == null) {
                return criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), startDate);
            }
            return criteriaBuilder.between(root.get("createdAt"), startDate, endDate);
        };
    }

    /**
     * Filter orders with delivery date between dates
     * <p>
     * Usage: deliveryBetween(start, end)
     * SQL: WHERE delivery_date BETWEEN ? AND ?
     *
     * @param startDate Start date
     * @param endDate   End date
     * @return Specification for delivery date range
     */
    public static Specification<Order> deliveryBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return (root, query, criteriaBuilder) -> {
            if (startDate == null && endDate == null) {
                return criteriaBuilder.conjunction();
            }
            if (startDate == null) {
                return criteriaBuilder.lessThanOrEqualTo(root.get("deliveryDate"), endDate);
            }
            if (endDate == null) {
                return criteriaBuilder.greaterThanOrEqualTo(root.get("deliveryDate"), startDate);
            }
            return criteriaBuilder.between(root.get("deliveryDate"), startDate, endDate);
        };
    }

    // ============================================================================
    // AMOUNT SPECIFICATIONS
    // ============================================================================

    /**
     * Filter orders with total amount greater than value
     * <p>
     * Usage: totalAmountGreaterThan(BigDecimal.valueOf(100))
     * SQL: WHERE total_amount > 100
     *
     * @param amount Minimum amount
     * @return Specification for amount greater than
     */
    public static Specification<Order> totalAmountGreaterThan(BigDecimal amount) {
        return (root, query, criteriaBuilder) -> {
            if (amount == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThan(root.get("totalAmount"), amount);
        };
    }

    /**
     * Filter orders with total amount less than value
     * <p>
     * Usage: totalAmountLessThan(BigDecimal.valueOf(1000))
     * SQL: WHERE total_amount < 1000
     *
     * @param amount Maximum amount
     * @return Specification for amount less than
     */
    public static Specification<Order> totalAmountLessThan(BigDecimal amount) {
        return (root, query, criteriaBuilder) -> {
            if (amount == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThan(root.get("totalAmount"), amount);
        };
    }

    /**
     * Filter orders with total amount between values
     * <p>
     * Usage: totalAmountBetween(BigDecimal.valueOf(100), BigDecimal.valueOf(1000))
     * SQL: WHERE total_amount BETWEEN 100 AND 1000
     *
     * @param minAmount Minimum amount
     * @param maxAmount Maximum amount
     * @return Specification for amount range
     */
    public static Specification<Order> totalAmountBetween(BigDecimal minAmount, BigDecimal maxAmount) {
        return (root, query, criteriaBuilder) -> {
            if (minAmount == null && maxAmount == null) {
                return criteriaBuilder.conjunction();
            }
            if (minAmount == null) {
                return criteriaBuilder.lessThanOrEqualTo(root.get("totalAmount"), maxAmount);
            }
            if (maxAmount == null) {
                return criteriaBuilder.greaterThanOrEqualTo(root.get("totalAmount"), minAmount);
            }
            return criteriaBuilder.between(root.get("totalAmount"), minAmount, maxAmount);
        };
    }

    // ============================================================================
    // JOIN SPECIFICATIONS (Customer)
    // ============================================================================

    /**
     * Filter orders by customer name (JOIN query)
     * <p>
     * Usage: customerNameContains("John")
     * SQL: SELECT o.* FROM orders o
     * JOIN customers c ON o.customer_id = c.id
     * WHERE c.name LIKE '%John%'
     *
     * @param name Customer name search term
     * @return Specification for customer name search
     */
    public static Specification<Order> customerNameContains(String name) {
        return (root, query, criteriaBuilder) -> {
            if (name == null || name.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            Join<Order, Customer> customerJoin = root.join("customer", JoinType.INNER);
            return criteriaBuilder.like(
                    criteriaBuilder.lower(customerJoin.get("name")),
                    "%" + name.toLowerCase() + "%"
            );
        };
    }

    /**
     * Filter orders by customer city (JOIN query)
     * <p>
     * Usage: customerCity("Istanbul")
     * SQL: SELECT o.* FROM orders o
     * JOIN customers c ON o.customer_id = c.id
     * WHERE c.city = 'Istanbul'
     *
     * @param city Customer city
     * @return Specification for customer city
     */
    public static Specification<Order> customerCity(String city) {
        return (root, query, criteriaBuilder) -> {
            if (city == null || city.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            Join<Order, Customer> customerJoin = root.join("customer", JoinType.INNER);
            return criteriaBuilder.equal(customerJoin.get("city"), city);
        };
    }

    /**
     * Filter orders by customer email (JOIN query)
     * <p>
     * Usage: customerEmail("john@example.com")
     * SQL: SELECT o.* FROM orders o
     * JOIN customers c ON o.customer_id = c.id
     * WHERE c.email = 'john@example.com'
     *
     * @param email Customer email
     * @return Specification for customer email
     */
    public static Specification<Order> customerEmail(String email) {
        return (root, query, criteriaBuilder) -> {
            if (email == null || email.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            Join<Order, Customer> customerJoin = root.join("customer", JoinType.INNER);
            return criteriaBuilder.equal(customerJoin.get("email"), email);
        };
    }

    // ============================================================================
    // STRING SEARCH SPECIFICATIONS
    // ============================================================================

    /**
     * Filter orders by order number (partial match)
     * <p>
     * Usage: orderNumberContains("ORD-2024")
     * SQL: WHERE order_number LIKE '%ORD-2024%'
     *
     * @param orderNumber Order number search term
     * @return Specification for order number search
     */
    public static Specification<Order> orderNumberContains(String orderNumber) {
        return (root, query, criteriaBuilder) -> {
            if (orderNumber == null || orderNumber.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("orderNumber")),
                    "%" + orderNumber.toLowerCase() + "%"
            );
        };
    }

    /**
     * Filter orders by shipping city
     * <p>
     * Usage: shippingCity("Ankara")
     * SQL: WHERE shipping_city = 'Ankara'
     *
     * @param city Shipping city
     * @return Specification for shipping city
     */
    public static Specification<Order> shippingCity(String city) {
        return (root, query, criteriaBuilder) -> {
            if (city == null || city.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("shippingCity"), city);
        };
    }

    /**
     * Filter orders by payment method
     * <p>
     * Usage: paymentMethod("CREDIT_CARD")
     * SQL: WHERE payment_method = 'CREDIT_CARD'
     *
     * @param paymentMethod Payment method
     * @return Specification for payment method
     */
    public static Specification<Order> paymentMethod(String paymentMethod) {
        return (root, query, criteriaBuilder) -> {
            if (paymentMethod == null || paymentMethod.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("paymentMethod"), paymentMethod);
        };
    }

    // ============================================================================
    // COMPLEX SPECIFICATIONS (Demonstration purposes)
    // ============================================================================

    /**
     * Complex specification: Active orders
     * Active = PENDING OR PROCESSING OR SHIPPED
     * <p>
     * Usage: isActive()
     * SQL: WHERE status IN ('PENDING', 'PROCESSING', 'SHIPPED')
     *
     * @return Specification for active orders
     */
    public static Specification<Order> isActive() {
        return (root, query, criteriaBuilder) ->
                root.get("status").in(
                        OrderStatus.PENDING,
                        OrderStatus.PROCESSING,
                        OrderStatus.SHIPPED
                );
    }

    /**
     * Complex specification: High-value orders
     * High-value = amount > 1000 AND status != CANCELLED
     * <p>
     * Usage: isHighValue()
     * SQL: WHERE total_amount > 1000 AND status != 'CANCELLED'
     *
     * @return Specification for high-value orders
     */
    public static Specification<Order> isHighValue() {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.and(
                        criteriaBuilder.greaterThan(root.get("totalAmount"), BigDecimal.valueOf(1000)),
                        criteriaBuilder.notEqual(root.get("status"), OrderStatus.CANCELLED)
                );
    }

    /**
     * Complex specification: Recent orders
     * Recent = created in last 30 days AND not cancelled
     * <p>
     * Usage: isRecent()
     * SQL: WHERE created_at > (NOW() - INTERVAL 30 DAY) AND status != 'CANCELLED'
     *
     * @return Specification for recent orders
     */
    public static Specification<Order> isRecent() {
        return (root, query, criteriaBuilder) -> {
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            return criteriaBuilder.and(
                    criteriaBuilder.greaterThan(root.get("createdAt"), thirtyDaysAgo),
                    criteriaBuilder.notEqual(root.get("status"), OrderStatus.CANCELLED)
            );
        };
    }
}