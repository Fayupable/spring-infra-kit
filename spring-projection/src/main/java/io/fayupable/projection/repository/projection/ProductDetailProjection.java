package io.fayupable.projection.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Product Detail Projection
 * <p>
 * Medium-weight interface for product detail page.
 * <p>
 * Purpose:
 * When user clicks on a product, we need MORE info than list view,
 * but STILL not everything (e.g., no analytics fields).
 * <p>
 * We need:
 * - All list view fields
 * - Description (for detail section)
 * - Stock (show availability)
 * - Full image URL (for zoom feature)
 * - Specifications (show specs table)
 * <p>
 * We DON'T need:
 * - View count (analytics only)
 * - Sales count (analytics only)
 * - Created/updated timestamps (not shown to customers)
 * <p>
 * Performance Impact:
 * Full Entity: ~2KB (all 14 fields)
 * Detail Projection: ~1.5KB (10 fields)
 * Result: 25% data reduction
 * <p>
 * SQL Generated:
 * SELECT p.id, p.name, p.brand, p.category, p.price, p.rating,
 * p.description, p.stock, p.image_url, p.specifications
 * FROM products p
 * WHERE p.id = ?
 */
public interface ProductDetailProjection {

    /**
     * Product ID
     */
    Long getId();

    /**
     * Product Name
     */
    String getName();

    /**
     * Brand Name
     */
    String getBrand();

    /**
     * Category
     * Used for: Breadcrumb navigation
     * Example: Home > Electronics > Smartphones
     */
    String getCategory();

    /**
     * Price
     */
    BigDecimal getPrice();

    /**
     * Average Rating
     */
    BigDecimal getRating();

    /**
     * Full Description
     * INCLUDED in detail view (excluded in list view)
     * Used for: Product description section
     */
    String getDescription();

    /**
     * Stock Quantity
     * INCLUDED in detail view
     * Used for: Show exact quantity or "Only 3 left!" message
     */
    Integer getStock();

    /**
     * Full-Size Image URL
     * INCLUDED in detail view (list view uses thumbnail)
     * Used for: Large product image with zoom feature
     */
    String getImageUrl();

    /**
     * Technical Specifications
     * INCLUDED in detail view (excluded in list view)
     * Used for: Specifications table
     * Example: "Display: 6.7 inch, RAM: 8GB, Storage: 256GB"
     */
    String getSpecifications();
}