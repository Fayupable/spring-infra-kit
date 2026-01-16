package io.fayupable.specification.enums;

/**
 * Order Status Enum
 *
 * Represents the lifecycle of an order from creation to completion.
 *
 * Status Flow:
 * PENDING → PROCESSING → SHIPPED → DELIVERED
 *         ↓
 *     CANCELLED
 */
public enum OrderStatus {
    /**
     * Order placed but not yet processed
     * Initial state after order creation
     */
    PENDING,

    /**
     * Order is being prepared
     * Payment confirmed, items being packed
     */
    PROCESSING,

    /**
     * Order shipped to customer
     * Tracking number available
     */
    SHIPPED,

    /**
     * Order delivered to customer
     * Final successful state
     */
    DELIVERED,

    /**
     * Order cancelled
     * Can happen from PENDING or PROCESSING state
     */
    CANCELLED,

    COMPLETED
}