package io.fayupable.specification.entity;

import io.fayupable.specification.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Order Entity
 *
 * Core entity for demonstrating Specification pattern.
 * Contains various fields for complex filtering scenarios.
 *
 * Specification Use Cases:
 * 1. Simple filters: status, date range, amount range
 * 2. JOIN queries: customer name, customer city
 * 3. Complex AND/OR: (status=PENDING OR status=PROCESSING) AND amount>100
 * 4. Date comparisons: createdAt, deliveryDate
 * 5. String searches: orderNumber LIKE, customer.name LIKE
 *
 * Performance Demo:
 * WITHOUT Spec: Load 1000 orders → Filter in Java → 850ms
 * WITH Spec: SQL WHERE clause → 45ms
 * Result: 19x faster
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "orders",
        indexes = {
                @Index(name = "idx_orders_status", columnList = "status"),
                @Index(name = "idx_orders_created_at", columnList = "created_at"),
                @Index(name = "idx_orders_delivery_date", columnList = "delivery_date"),
                @Index(name = "idx_orders_total_amount", columnList = "total_amount"),
                @Index(name = "idx_orders_customer_id", columnList = "customer_id"),
                @Index(name = "idx_orders_order_number", columnList = "order_number")
        }
)
public class Order {

    /**
     * Primary Key
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Order Number
     * Unique identifier for customer reference
     * Format: ORD-YYYYMMDD-XXXX
     * Example: ORD-20250116-0001
     */
    @Column(name = "order_number", nullable = false, unique = true, length = 50)
    private String orderNumber;

    /**
     * Customer who placed the order
     * ManyToOne relationship for JOIN queries
     *
     * Specification Usage:
     * - Filter by customer name: JOIN customers WHERE name LIKE '%John%'
     * - Filter by customer city: JOIN customers WHERE city = 'Istanbul'
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false, foreignKey = @ForeignKey(name = "fk_orders_customer"))
    private Customer customer;

    /**
     * Order Status
     * Primary filter field for specifications
     *
     * Common Queries:
     * - status = COMPLETED
     * - status IN (PENDING, PROCESSING)
     * - status != CANCELLED
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    /**
     * Total Order Amount
     * Used for range queries
     *
     * Common Queries:
     * - totalAmount > 100
     * - totalAmount BETWEEN 100 AND 1000
     * - totalAmount <= 500
     */
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    /**
     * Order Creation Date
     * Primary date filter field
     *
     * Common Queries:
     * - createdAt > '2024-01-01'
     * - createdAt BETWEEN '2024-01-01' AND '2024-12-31'
     * - createdAt < NOW() - 30 days
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Expected Delivery Date
     * Used for delivery tracking filters
     *
     * Common Queries:
     * - deliveryDate BETWEEN today AND today+7days
     * - deliveryDate < '2024-06-01'
     */
    @Column(name = "delivery_date")
    private LocalDateTime deliveryDate;

    /**
     * Shipping Address
     */
    @Column(name = "shipping_address", length = 500)
    private String shippingAddress;

    /**
     * Shipping City
     * Secondary location filter (customer.city is primary)
     */
    @Column(name = "shipping_city", length = 100)
    private String shippingCity;

    /**
     * Payment Method
     */
    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    /**
     * Tracking Number
     * Available after SHIPPED status
     */
    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;

    /**
     * Order Notes
     * Customer comments or special instructions
     */
    @Column(length = 1000)
    private String notes;

    /**
     * Last Update Timestamp
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Order Items
     * OneToMany relationship
     * Not heavily used in specifications (focus on Order-level filters)
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Helper method: Add item to order
     */
    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    /**
     * Helper method: Calculate total from items
     */
    public void calculateTotal() {
        this.totalAmount = items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}