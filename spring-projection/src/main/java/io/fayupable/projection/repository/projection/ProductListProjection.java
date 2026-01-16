package io.fayupable.projection.repository.projection;

import java.math.BigDecimal;

/**
 * Product List Projection
 * <p>
 * Lightweight interface for product list views (e.g., catalog page, search results).
 * <p>
 * Purpose:
 * When showing 50-200 products on a page, we DON'T need:
 * - Full description (2000 chars)
 * - Full image URL
 * - Specifications (1000 chars)
 * - View/sales counts
 * - Audit timestamps
 * <p>
 * We ONLY need:
 * - ID (for navigation)
 * - Name (display)
 * - Brand (display)
 * - Price (display)
 * - Rating (display stars)
 * - Thumbnail (show image)
 * <p>
 * Performance Impact:
 * WITHOUT Projection: ~2KB per product x 200 = 400KB
 * WITH Projection: ~200 bytes per product x 200 = 40KB
 * Result: 90% bandwidth saved, 10x faster page load
 * <p>
 * SQL Generated:
 * SELECT p.id, p.name, p.brand, p.price, p.rating, p.thumbnail_url
 * FROM products p
 * (instead of SELECT * FROM products)
 * <p>
 * Spring Data JPA automatically:
 * 1. Detects this is a projection interface
 * 2. Generates SQL with only these fields
 * 3. Maps result directly to interface (no entity instantiation)
 */
public interface ProductListProjection {

    /**
     * Product ID
     * Used for: Navigation to detail page
     */
    Long getId();

    /**
     * Product Name
     * Used for: Display in card title
     * Example: "iPhone 15 Pro Max"
     */
    String getName();

    /**
     * Brand Name
     * Used for: Display below product name
     * Example: "Apple"
     */
    String getBrand();

    /**
     * Price
     * Used for: Display in card
     * Example: $1,299.99
     */
    BigDecimal getPrice();

    /**
     * Average Rating
     * Used for: Display star rating
     * Example: 4.5 ⭐
     */
    BigDecimal getRating();

    /**
     * Thumbnail Image URL
     * Used for: Display small product image
     * Example: "https://cdn.example.com/thumb/iphone.jpg"
     */
    String getThumbnailUrl();
}