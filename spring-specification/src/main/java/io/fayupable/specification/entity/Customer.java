package io.fayupable.specification.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Customer Entity
 * <p>
 * Represents a customer in the e-commerce system.
 * One customer can have multiple orders.
 * <p>
 * Relationship:
 * Customer (1) → Orders (N)
 * <p>
 * Used for demonstrating JOIN queries in specifications:
 * - Filter orders by customer name
 * - Filter orders by customer city
 * - Filter orders by customer email
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "customers",
        indexes = {
                @Index(name = "idx_customers_email", columnList = "email"),
                @Index(name = "idx_customers_city", columnList = "city"),
                @Index(name = "idx_customers_name", columnList = "name")
        }
)
public class Customer {

    /**
     * Primary Key
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Customer Name
     * Used in JOIN queries: "Find orders where customer name contains 'John'"
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * Customer Email
     * Unique identifier for customer
     */
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    /**
     * Customer Phone
     */
    @Column(length = 20)
    private String phone;

    /**
     * Customer City
     * Used for location-based filtering: "Find orders from customers in Istanbul"
     */
    @Column(length = 100)
    private String city;

    /**
     * Customer Address
     */
    @Column(length = 500)
    private String address;

    /**
     * Customer Registration Date
     */
    @Column(name = "registered_at", nullable = false, updatable = false)
    private LocalDateTime registeredAt;

    /**
     * Orders belonging to this customer
     * Lazy loading for performance
     */
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Order> orders = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        registeredAt = LocalDateTime.now();
    }

    /**
     * Helper method: Add order to customer
     */
    public void addOrder(Order order) {
        orders.add(order);
        order.setCustomer(this);
    }
}