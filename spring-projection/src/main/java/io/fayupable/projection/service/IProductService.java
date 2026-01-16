package io.fayupable.projection.service;

import io.fayupable.projection.entity.Product;
import io.fayupable.projection.repository.ProductRepository;
import io.fayupable.projection.repository.projection.ProductListProjection;
import io.fayupable.projection.repository.projection.ProductDetailProjection;
import io.fayupable.projection.repository.projection.ProductSummaryProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

/**
 * Product Service Interface
 * <p>
 * Defines business operations for product management.
 * Demonstrates projection usage patterns for different use cases.
 * <p>
 * Key Principles:
 * 1. Use appropriate projection for each use case
 * 2. Never fetch more data than needed
 * 3. Full entities only when genuinely needed (updates, modifications)
 * <p>
 * Method Naming Convention:
 * - getXxx() - Returns full entity
 * - getXxxList() - Returns list projection
 * - getXxxDetail() - Returns detail projection
 * - getXxxSummary() - Returns summary projection
 * <p>
 * Performance Impact:
 * Choosing correct projection can improve response time by 10-40x.
 */
public interface IProductService {

    // ============================================================================
    // FULL ENTITY METHODS (When you need everything)
    // ============================================================================

    /**
     * Get all products - FULL ENTITY
     * <p>
     * WARNING: Returns complete Product entities.
     * Use ONLY when you genuinely need all fields (rare).
     * <p>
     * Performance: HEAVY
     * - 200 products x 2KB = 400KB transferred
     * - Slow database query (SELECT *)
     * - High memory usage
     * <p>
     * Better alternative: Use getAllProductsList() for most cases
     *
     * @return List of complete Product entities
     */
    List<Product> getAllProducts();

    /**
     * Get product by ID - FULL ENTITY
     * <p>
     * Returns complete product entity.
     * Use when you need to UPDATE the product (need full entity for JPA).
     * <p>
     * For READ-ONLY detail view, use getProductDetail() instead.
     *
     * @param id Product ID
     * @return Complete Product entity
     * @throws RuntimeException if product not found
     */
    Product getProductById(Long id);

    /**
     * Get products by category - FULL ENTITY
     * <p>
     * Returns full entities.
     * Use when you need to modify products after fetching.
     *
     * @param category Category name
     * @return List of complete Product entities
     */
    List<Product> getProductsByCategory(String category);

    // ============================================================================
    // LIST PROJECTION METHODS (For catalog/list views)
    // ============================================================================

    /**
     * Get all products - LIST PROJECTION
     * <p>
     * Returns lightweight projections for product catalog.
     * This is the RECOMMENDED method for list views.
     * <p>
     * Performance: LIGHT
     * - 200 products x 200 bytes = 40KB transferred
     * - Fast database query (SELECT specific columns)
     * - Low memory usage
     * <p>
     * Fields returned: id, name, brand, price, rating, thumbnailUrl
     * Fields excluded: description, specifications, stock, analytics
     * <p>
     * Result: 90% faster than getAllProducts()
     *
     * @return List of ProductListProjection
     */
    List<ProductListProjection> getAllProductsList();

    /**
     * Get products by category - LIST PROJECTION
     * <p>
     * Paginated list for category pages.
     * <p>
     * Use case: Electronics category showing 20 products per page
     * <p>
     * Performance:
     * - Full entity: 20 x 2KB = 40KB per page
     * - List projection: 20 x 200 bytes = 4KB per page
     * - Result: 90% reduction
     *
     * @param category Category name
     * @param pageable Pagination parameters
     * @return Page of ProductListProjection
     */
    Page<ProductListProjection> getProductsByCategoryPaged(String category, Pageable pageable);

    /**
     * Get products by brand - LIST PROJECTION
     * <p>
     * Returns products sorted by rating (highest first).
     * <p>
     * Use case: Apple products page
     *
     * @param brand Brand name
     * @return List of ProductListProjection sorted by rating
     */
    List<ProductListProjection> getProductsByBrand(String brand);

    /**
     * Get products in price range - LIST PROJECTION
     * <p>
     * Use case: Filter products between $100-$500
     *
     * @param minPrice Minimum price
     * @param maxPrice Maximum price
     * @return List of ProductListProjection in price range
     */
    List<ProductListProjection> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice);

    /**
     * Get top-rated products - LIST PROJECTION
     * <p>
     * Returns top 10 products with rating >= 4.5
     * <p>
     * Use case: "Top Rated" section on homepage
     *
     * @return Top 10 highest-rated products
     */
    List<ProductListProjection> getTopRatedProducts();

    /**
     * Filter products - LIST PROJECTION
     * <p>
     * Advanced filtering with multiple criteria.
     * <p>
     * Use case: Category + Brand + Price range filter on catalog page
     *
     * @param category Category name
     * @param brand    Brand name
     * @param minPrice Minimum price
     * @param maxPrice Maximum price
     * @return Filtered list of ProductListProjection
     */
    List<ProductListProjection> filterProducts(
            String category,
            String brand,
            BigDecimal minPrice,
            BigDecimal maxPrice
    );

    // ============================================================================
    // DETAIL PROJECTION METHODS (For detail pages)
    // ============================================================================

    /**
     * Get product detail - DETAIL PROJECTION
     * <p>
     * Returns medium-weight projection for product detail page.
     * Includes description and specifications (excluded from list view).
     * <p>
     * Performance: MEDIUM
     * - Full entity: ~2KB
     * - Detail projection: ~1.5KB
     * - Result: 25% faster
     * <p>
     * Fields returned: id, name, brand, category, price, rating,
     * description, stock, imageUrl, specifications
     * Fields excluded: viewCount, salesCount, createdAt, updatedAt
     * <p>
     * Use case: User clicks product from list to see details
     *
     * @param id Product ID
     * @return ProductDetailProjection
     * @throws RuntimeException if product not found
     */
    ProductDetailProjection getProductDetail(Long id);

    // ============================================================================
    // SUMMARY PROJECTION METHODS (For minimal views)
    // ============================================================================

    /**
     * Get products by IDs - SUMMARY PROJECTION
     * <p>
     * Ultra-lightweight projection for shopping cart, order history.
     * <p>
     * Performance: ULTRA-LIGHT
     * - Full entity: 20 items x 2KB = 40KB
     * - Summary projection: 20 items x 50 bytes = 1KB
     * - Result: 97.5% reduction, 40x faster
     * <p>
     * Fields returned: id, name, price (ONLY!)
     * Fields excluded: Everything else
     * <p>
     * Use case: Shopping cart with 20 items
     *
     * @param productIds List of product IDs
     * @return List of ProductSummaryProjection
     */
    List<ProductSummaryProjection> getProductsSummary(List<Long> productIds);

    /**
     * Search products - SUMMARY PROJECTION
     * <p>
     * Quick search for autocomplete.
     * <p>
     * Use case: User types "iPhone" in search box
     * - Show top 5 matching products
     * - Only need name and price (not full details)
     *
     * @param keyword Search keyword
     * @return Top 5 matching products (summary)
     */
    List<ProductSummaryProjection> searchProducts(String keyword);

    // ============================================================================
    // STATISTICS METHODS (Aggregate projections)
    // ============================================================================

    /**
     * Get category statistics
     * <p>
     * Returns aggregate data for analytics dashboard.
     * <p>
     * Use case: Admin dashboard showing:
     * - Electronics: 45 products, avg price $850, avg rating 4.3
     * - Clothing: 120 products, avg price $65, avg rating 4.1
     *
     * @return List of category statistics
     */
    List<ProductRepository.CategoryStatsProjection> getCategoryStatistics();

    // ============================================================================
    // COMPARISON METHOD (Demonstrate performance difference)
    // ============================================================================

    /**
     * Compare performance - FULL ENTITY vs PROJECTION
     * <p>
     * This method demonstrates the performance difference.
     * Returns timing data for comparison.
     * <p>
     * Use case: Performance benchmark for documentation
     *
     * @return ComparisonResult with timing and size metrics
     */
    ComparisonResult comparePerformance();

    /**
     * Comparison Result DTO
     * <p>
     * Contains performance metrics comparing full entity vs projection queries.
     */
    record ComparisonResult(
            long fullEntityDurationMs,
            int fullEntitySizeBytes,
            long projectionDurationMs,
            int projectionSizeBytes,
            double speedImprovement,
            double sizeReductionPercent
    ) {
    }
}