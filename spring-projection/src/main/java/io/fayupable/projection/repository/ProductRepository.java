package io.fayupable.projection.repository;


import io.fayupable.projection.entity.Product;
import io.fayupable.projection.repository.projection.ProductDetailProjection;
import io.fayupable.projection.repository.projection.ProductListProjection;
import io.fayupable.projection.repository.projection.ProductSummaryProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Product Repository
 * <p>
 * Demonstrates different ways to use JPA Projections.
 * <p>
 * Key Concept:
 * Same entity (Product), different projections for different use cases.
 * <p>
 * Repository Methods Pattern:
 * 1. Full Entity methods - Fetch everything (heavy)
 * 2. List Projection methods - Fetch only list view fields (light)
 * 3. Detail Projection methods - Fetch detail view fields (medium)
 * 4. Summary Projection methods - Fetch minimal fields (ultra-light)
 * <p>
 * Performance Comparison:
 * - findAll() returns List<Product> ~2KB each
 * - findAllProjectedBy() returns List<ProductListProjection> ~200 bytes each
 * - Result: 10x bandwidth reduction
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // ============================================================================
    // FULL ENTITY QUERIES (Without Projection)
    // ============================================================================
    // These are inherited from JpaRepository:
    // - findAll()
    // - findById(Long id)
    // - save(Product product)
    // SQL: SELECT * FROM products (ALL 14 fields)

    /**
     * Find products by category - FULL ENTITY
     * <p>
     * Returns complete Product entities with all fields.
     * Use case: When you genuinely need ALL product data.
     * <p>
     * Performance: HEAVY
     * - Fetches description (2000 chars)
     * - Fetches specifications (1000 chars)
     * - Fetches analytics fields
     * <p>
     * SQL Generated:
     * SELECT * FROM products WHERE category = ?
     */
    List<Product> findByCategory(String category);

    /**
     * Find products by brand - FULL ENTITY
     * <p>
     * Returns complete Product entities.
     * <p>
     * SQL Generated:
     * SELECT * FROM products WHERE brand = ?
     */
    List<Product> findByBrand(String brand);

    // ============================================================================
    // LIST PROJECTION QUERIES (Lightweight for Lists)
    // ============================================================================

    /**
     * Find all products - LIST PROJECTION
     * <p>
     * Returns only fields needed for product list view.
     * This is the MAIN method for catalog pages.
     * <p>
     * Projection Fields: id, name, brand, price, rating, thumbnailUrl
     * Excluded Fields: description, specifications, imageUrl, stock, analytics
     * <p>
     * Performance Impact:
     * - Full Entity: 200 products x 2KB = 400KB
     * - List Projection: 200 products x 200 bytes = 40KB
     * - Result: 90% bandwidth saved
     * <p>
     * SQL Generated:
     * SELECT p.id, p.name, p.brand, p.price, p.rating, p.thumbnail_url
     * FROM products p
     */
    List<ProductListProjection> findAllProjectedBy();

    /**
     * Find products by category - LIST PROJECTION
     * <p>
     * Paginated version for category pages.
     * <p>
     * Use case: Electronics page showing 20 products per page
     * <p>
     * SQL Generated:
     * SELECT p.id, p.name, p.brand, p.price, p.rating, p.thumbnail_url
     * FROM products p
     * WHERE p.category = ?
     * LIMIT ? OFFSET ?
     */
    Page<ProductListProjection> findByCategory(String category, Pageable pageable);

    /**
     * Find products by brand - LIST PROJECTION
     * <p>
     * Use case: Apple products page
     * <p>
     * SQL Generated:
     * SELECT p.id, p.name, p.brand, p.price, p.rating, p.thumbnail_url
     * FROM products p
     * WHERE p.brand = ?
     */
    List<ProductListProjection> findByBrandOrderByRatingDesc(String brand);

    /**
     * Find products in price range - LIST PROJECTION
     * <p>
     * Use case: Filter products between $100-$500
     * <p>
     * SQL Generated:
     * SELECT p.id, p.name, p.brand, p.price, p.rating, p.thumbnail_url
     * FROM products p
     * WHERE p.price BETWEEN ? AND ?
     */
    List<ProductListProjection> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);

    /**
     * Find top-rated products - LIST PROJECTION
     * <p>
     * Use case: "Top Rated" section on homepage
     * <p>
     * SQL Generated:
     * SELECT p.id, p.name, p.brand, p.price, p.rating, p.thumbnail_url
     * FROM products p
     * WHERE p.rating >= ?
     * ORDER BY p.rating DESC
     */
    List<ProductListProjection> findTop10ByRatingGreaterThanEqualOrderByRatingDesc(BigDecimal minRating);

    // ============================================================================
    // DETAIL PROJECTION QUERIES (Medium-weight for Detail Pages)
    // ============================================================================

    /**
     * Find product by ID - DETAIL PROJECTION
     * <p>
     * Returns fields needed for product detail page.
     * Includes description and specifications (excluded from list view).
     * <p>
     * Projection Fields: id, name, brand, category, price, rating,
     * description, stock, imageUrl, specifications
     * Excluded Fields: viewCount, salesCount, createdAt, updatedAt
     * <p>
     * Performance Impact:
     * - Full Entity: ~2KB
     * - Detail Projection: ~1.5KB
     * - Result: 25% data reduction
     * <p>
     * SQL Generated:
     * SELECT p.id, p.name, p.brand, p.category, p.price, p.rating,
     * p.description, p.stock, p.image_url, p.specifications
     * FROM products p
     * WHERE p.id = ?
     */
    Optional<ProductDetailProjection> findProjectedById(Long id);

    // ============================================================================
    // SUMMARY PROJECTION QUERIES (Ultra-lightweight for Minimal Views)
    // ============================================================================

    /**
     * Find products by IDs - SUMMARY PROJECTION
     * <p>
     * Ultra-minimal projection for shopping cart, order history.
     * <p>
     * Projection Fields: id, name, price (ONLY 3 fields!)
     * Excluded Fields: Everything else
     * <p>
     * Use case: Shopping cart with 20 items
     * - Full entities: 20 x 2KB = 40KB
     * - Summary projection: 20 x 50 bytes = 1KB
     * - Result: 97.5% reduction, 40x faster
     * <p>
     * SQL Generated:
     * SELECT p.id, p.name, p.price
     * FROM products p
     * WHERE p.id IN (?, ?, ?)
     */
    List<ProductSummaryProjection> findByIdIn(List<Long> ids);

    /**
     * Search products by name - SUMMARY PROJECTION
     * <p>
     * Use case: Autocomplete search box
     * <p>
     * When user types "iPhone", show quick suggestions.
     * No need for full data, just name and price.
     * <p>
     * SQL Generated:
     * SELECT p.id, p.name, p.price
     * FROM products p
     * WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', ?, '%'))
     */
    List<ProductSummaryProjection> findTop5ByNameContainingIgnoreCase(String keyword);

    // ============================================================================
    // CUSTOM QUERIES WITH PROJECTIONS
    // ============================================================================

    /**
     * Find products with filters - LIST PROJECTION
     * <p>
     * Custom JPQL query with multiple filters.
     * Returns list projection for catalog page.
     * <p>
     * Use case: Category + Brand + Price range filter
     * <p>
     * SQL Generated:
     * SELECT p.id, p.name, p.brand, p.price, p.rating, p.thumbnail_url
     * FROM products p
     * WHERE p.category = ?
     * AND p.brand = ?
     * AND p.price BETWEEN ? AND ?
     */
    @Query("SELECT p.id as id, p.name as name, p.brand as brand, " +
            "p.price as price, p.rating as rating, p.thumbnailUrl as thumbnailUrl " +
            "FROM Product p " +
            "WHERE p.category = :category " +
            "AND p.brand = :brand " +
            "AND p.price BETWEEN :minPrice AND :maxPrice")
    List<ProductListProjection> findFilteredProducts(
            @Param("category") String category,
            @Param("brand") String brand,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice
    );

    /**
     * Get product statistics by category - CUSTOM PROJECTION
     * <p>
     * Aggregate query for analytics.
     * Returns category name and average price.
     * <p>
     * Use case: Admin dashboard showing category insights
     */
    @Query("SELECT p.category as category, " +
            "COUNT(p) as productCount, " +
            "AVG(p.price) as averagePrice, " +
            "AVG(p.rating) as averageRating " +
            "FROM Product p " +
            "GROUP BY p.category " +
            "ORDER BY COUNT(p) DESC")
    List<CategoryStatsProjection> getCategoryStatistics();

    /**
     * Category Statistics Projection
     * Used for aggregate queries.
     */
    interface CategoryStatsProjection {
        String getCategory();

        Long getProductCount();

        Double getAveragePrice();

        Double getAverageRating();
    }
}