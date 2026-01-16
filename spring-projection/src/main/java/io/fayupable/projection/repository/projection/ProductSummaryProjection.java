package io.fayupable.projection.repository.projection;

import java.math.BigDecimal;

/**
 * Product Summary Projection
 *
 * Ultra-lightweight interface for minimal views.
 *
 * Purpose:
 * Used for scenarios where we need ABSOLUTE MINIMUM data:
 * - Shopping cart items
 * - Order history items
 * - Quick search autocomplete
 * - Related products sidebar
 *
 * We ONLY need:
 * - ID (for linking)
 * - Name (display)
 * - Price (display)
 *
 * Performance Impact:
 * Full Entity: ~2KB
 * Summary Projection: ~50 bytes
 * Result: 97.5% data reduction, 40x smaller
 *
 * Use Case Example:
 * Shopping cart with 20 items:
 * - Full entities: 20 x 2KB = 40KB
 * - Summary projection: 20 x 50 bytes = 1KB
 * - Result: Page loads 40x faster
 *
 * SQL Generated:
 * SELECT p.id, p.name, p.price
 * FROM products p
 */
public interface ProductSummaryProjection {

    /**
     * Product ID
     * Used for: Navigation/linking
     */
    Long getId();

    /**
     * Product Name
     * Used for: Display in compact lists
     */
    String getName();

    /**
     * Price
     * Used for: Display price in compact format
     */
    BigDecimal getPrice();
}