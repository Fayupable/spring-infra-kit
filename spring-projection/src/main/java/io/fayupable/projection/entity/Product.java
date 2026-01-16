package io.fayupable.projection.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Product Entity
 * <p>
 * Core entity representing an e-commerce product in the catalog.
 * Used to demonstrate JPA Projection pattern benefits.
 * <p>
 * Database Table: products
 * <p>
 * Key Features:
 * - Full product information for detail pages
 * - Heavy entity with multiple fields (simulates real-world scenario)
 * - Demonstrates performance impact of fetching all fields
 * <p>
 * Projection Demo Purpose:
 * This entity intentionally has many fields to show the difference between:
 * - Fetching full entity (all 14+ fields) for list views = WASTEFUL
 * - Using projections (only 4-5 fields) for list views = EFFICIENT
 * <p>
 * Real-world analogy:
 * You don't need full product description, specifications, and all images
 * just to show a product card in a list. Projections solve this problem.
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "products",
        indexes = {
                // Fast filtering by category (e.g., show all Electronics)
                @Index(name = "idx_products_category", columnList = "category"),
                // Fast filtering by brand (e.g., show all Apple products)
                @Index(name = "idx_products_brand", columnList = "brand"),
                // Fast price range queries (e.g., products between $100-$500)
                @Index(name = "idx_products_price", columnList = "price"),
                // Fast sorting by creation date (e.g., newest products first)
                @Index(name = "idx_products_created_at", columnList = "created_at")
        }
)
public class Product {

    /**
     * Primary Key
     * Auto-increment for simplicity
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Product Name
     * <p>
     * Required for all views (list and detail).
     * Always included in projections.
     * <p>
     * Example: "iPhone 15 Pro Max"
     */
    @Column(nullable = false, length = 255)
    private String name;

    /**
     * Product Description
     * <p>
     * HEAVY FIELD: 2000 characters of text.
     * This is the PRIMARY reason projections matter.
     * <p>
     * Required for: Detail page
     * NOT needed for: List view, search results, shopping cart
     * <p>
     * Without Projection: Fetched unnecessarily in list views
     * With Projection: Excluded from list queries
     * <p>
     * Performance Impact:
     * - 200 products x 2KB each = 400KB transferred
     * - With projection (exclude this): 40KB transferred
     * - 90% bandwidth saved!
     */
    @Column(length = 2000)
    private String description;

    /**
     * Brand Name
     * <p>
     * Required for both list and detail views.
     * Commonly used for filtering.
     * <p>
     * Example: "Apple", "Samsung", "Nike"
     */
    @Column(nullable = false, length = 100)
    private String brand;

    /**
     * Product Category
     * <p>
     * Used for navigation and filtering.
     * Always included in list projections.
     * <p>
     * Example: "Electronics", "Clothing", "Books"
     */
    @Column(nullable = false, length = 100)
    private String category;

    /**
     * Price
     * <p>
     * Critical field for list views.
     * Always included in projections.
     * <p>
     * precision = 10: total digits (e.g., 99999999.99)
     * scale = 2: decimal places (cents)
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /**
     * Stock Quantity
     * <p>
     * Used for: Detail page, inventory management
     * NOT needed for: List view (just show "In Stock" badge)
     * <p>
     * Projection Benefit:
     * - List view: Exclude this, just check if > 0
     * - Detail view: Include actual number
     */
    @Column(nullable = false)
    private Integer stock;

    /**
     * Average Rating
     * <p>
     * Commonly shown in list views (4.5 stars).
     * Lightweight numeric field.
     * <p>
     * precision = 3: total digits (e.g., 5.00)
     * scale = 2: decimal places (4.85)
     */
    @Column(precision = 3, scale = 2)
    private BigDecimal rating;

    /**
     * Full-Size Image URL
     * <p>
     * HEAVY FIELD: Not needed for list views.
     * <p>
     * Used for: Detail page, zoom feature
     * NOT needed for: List view (use thumbnail instead)
     * <p>
     * Example: "https://cdn.example.com/products/iphone-15-pro-max-full.jpg"
     */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /**
     * Thumbnail Image URL
     * <p>
     * Lightweight field for list views.
     * Always included in list projections.
     * <p>
     * Example: "https://cdn.example.com/products/iphone-15-pro-max-thumb.jpg"
     */
    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    /**
     * Technical Specifications
     * <p>
     * HEAVY FIELD: JSON or text with detailed specs.
     * <p>
     * Used for: Detail page (show full specs table)
     * NOT needed for: List view
     * <p>
     * Example: "RAM: 8GB, Storage: 256GB, Display: 6.7 inch OLED..."
     * <p>
     * Projection Benefit:
     * - Exclude this from list queries
     * - Load only when user clicks product
     */
    @Column(length = 1000)
    private String specifications;

    /**
     * View Count
     * <p>
     * Analytics field.
     * <p>
     * Used for: Admin dashboard, popularity sorting
     * NOT needed for: Customer-facing list views
     * <p>
     * Projection Benefit:
     * - Exclude from customer queries
     * - Include in admin/analytics projections
     */
    @Column(name = "view_count")
    @Builder.Default
    private Integer viewCount = 0;

    /**
     * Sales Count
     * <p>
     * Analytics field for tracking popularity.
     * <p>
     * Used for: Admin reports, "bestseller" badge
     * NOT needed for: Regular list views (unless showing badge)
     */
    @Column(name = "sales_count")
    @Builder.Default
    private Integer salesCount = 0;

    /**
     * Creation Timestamp
     * <p>
     * Audit field.
     * Used for: "New arrivals" section, admin reports
     * NOT needed for: Regular product list
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Last Update Timestamp
     * <p>
     * Audit field for tracking changes.
     * Rarely needed in customer-facing views.
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * JPA Lifecycle Callback: Before Persist
     * <p>
     * Automatically sets timestamps when product is first saved.
     */
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    /**
     * JPA Lifecycle Callback: Before Update
     * <p>
     * Automatically updates timestamp when product is modified.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Helper Method: Check Stock Availability
     * <p>
     * Business logic helper.
     * Used in projection queries to show "In Stock" badge.
     */
    public boolean isInStock() {
        return stock != null && stock > 0;
    }

    /**
     * Helper Method: Get Display Price
     * <p>
     * Formats price for display with currency symbol.
     * <p>
     * Example: $1,299.99
     */
    public String getDisplayPrice() {
        return String.format("$%,.2f", price);
    }

    /**
     * Helper Method: Increment View Count
     * <p>
     * Called when product detail page is viewed.
     * Used for analytics and popularity tracking.
     */
    public void incrementViewCount() {
        this.viewCount = (this.viewCount == null ? 0 : this.viewCount) + 1;
    }

    /**
     * Helper Method: Increment Sales Count
     * <p>
     * Called when product is purchased.
     * Used for analytics and bestseller tracking.
     */
    public void incrementSalesCount() {
        this.salesCount = (this.salesCount == null ? 0 : this.salesCount) + 1;
    }
}