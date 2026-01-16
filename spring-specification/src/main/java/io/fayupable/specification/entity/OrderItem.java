package io.fayupable.specification.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Order Item Entity
 *
 * Represents individual items in an order.
 * Not heavily used in Specification demo (focus is on Order-level filtering).
 *
 * Relationship:
 * Order (1) → OrderItems (N)
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "order_items")
public class OrderItem {

    /**
     * Primary Key
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Order this item belongs to
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, foreignKey = @ForeignKey(name = "fk_order_items_order"))
    private Order order;

    /**
     * Product Name
     */
    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    /**
     * Quantity
     */
    @Column(nullable = false)
    private Integer quantity;

    /**
     * Unit Price
     */
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    /**
     * Calculate subtotal
     */
    public BigDecimal getSubtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}