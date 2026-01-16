package io.fayupable.projection.service;

import io.fayupable.projection.entity.Product;
import io.fayupable.projection.repository.ProductRepository;
import io.fayupable.projection.repository.projection.ProductListProjection;
import io.fayupable.projection.repository.projection.ProductDetailProjection;
import io.fayupable.projection.repository.projection.ProductSummaryProjection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Product Service Implementation
 *
 * Implements business logic for product operations with projection patterns.
 *
 * Key Implementation Details:
 * - All read operations use @Transactional(readOnly = true) for optimization
 * - Comprehensive logging for performance monitoring
 * - Exception handling with meaningful messages
 * - Performance comparison methods for demonstration
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService implements IProductService {

    private final ProductRepository productRepository;

    // ============================================================================
    // FULL ENTITY IMPLEMENTATIONS
    // ============================================================================

    @Override
    public List<Product> getAllProducts() {
        log.info("Fetching ALL products (FULL ENTITY) - WARNING: Heavy operation");
        long startTime = System.currentTimeMillis();

        List<Product> products = productRepository.findAll();

        long duration = System.currentTimeMillis() - startTime;
        log.info("Fetched {} full products in {}ms", products.size(), duration);

        return products;
    }

    @Override
    public Product getProductById(Long id) {
        log.info("Fetching product {} (FULL ENTITY)", id);
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));
    }

    @Override
    public List<Product> getProductsByCategory(String category) {
        log.info("Fetching products by category: {} (FULL ENTITY)", category);
        return productRepository.findByCategory(category);
    }

    // ============================================================================
    // LIST PROJECTION IMPLEMENTATIONS
    // ============================================================================

    @Override
    public List<ProductListProjection> getAllProductsList() {
        log.info("Fetching all products (LIST PROJECTION)");
        long startTime = System.currentTimeMillis();

        List<ProductListProjection> products = productRepository.findAllProjectedBy();

        long duration = System.currentTimeMillis() - startTime;
        log.info("Fetched {} products (list view) in {}ms", products.size(), duration);

        return products;
    }

    @Override
    public Page<ProductListProjection> getProductsByCategoryPaged(String category, Pageable pageable) {
        log.info("Fetching products by category: {} (LIST PROJECTION, page: {})",
                category, pageable.getPageNumber());

        long startTime = System.currentTimeMillis();
        Page<ProductListProjection> products = productRepository.findByCategory(category, pageable);
        long duration = System.currentTimeMillis() - startTime;

        log.info("Fetched {} products in {}ms", products.getNumberOfElements(), duration);
        return products;
    }

    @Override
    public List<ProductListProjection> getProductsByBrand(String brand) {
        log.info("Fetching products by brand: {} (LIST PROJECTION)", brand);
        return productRepository.findByBrandOrderByRatingDesc(brand);
    }

    @Override
    public List<ProductListProjection> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        log.info("Fetching products in price range: ${} - ${} (LIST PROJECTION)", minPrice, maxPrice);
        return productRepository.findByPriceBetween(minPrice, maxPrice);
    }

    @Override
    public List<ProductListProjection> getTopRatedProducts() {
        log.info("Fetching top-rated products (LIST PROJECTION)");
        return productRepository.findTop10ByRatingGreaterThanEqualOrderByRatingDesc(
                BigDecimal.valueOf(4.5)
        );
    }

    @Override
    public List<ProductListProjection> filterProducts(
            String category,
            String brand,
            BigDecimal minPrice,
            BigDecimal maxPrice) {

        log.info("Filtering products - category: {}, brand: {}, price: ${}-${} (LIST PROJECTION)",
                category, brand, minPrice, maxPrice);

        return productRepository.findFilteredProducts(category, brand, minPrice, maxPrice);
    }

    // ============================================================================
    // DETAIL PROJECTION IMPLEMENTATIONS
    // ============================================================================

    @Override
    public ProductDetailProjection getProductDetail(Long id) {
        log.info("Fetching product {} (DETAIL PROJECTION)", id);

        long startTime = System.currentTimeMillis();
        ProductDetailProjection product = productRepository.findProjectedById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));
        long duration = System.currentTimeMillis() - startTime;

        log.info("Fetched product detail in {}ms", duration);
        return product;
    }

    // ============================================================================
    // SUMMARY PROJECTION IMPLEMENTATIONS
    // ============================================================================

    @Override
    public List<ProductSummaryProjection> getProductsSummary(List<Long> productIds) {
        log.info("Fetching {} products (SUMMARY PROJECTION)", productIds.size());

        long startTime = System.currentTimeMillis();
        List<ProductSummaryProjection> products = productRepository.findByIdIn(productIds);
        long duration = System.currentTimeMillis() - startTime;

        log.info("Fetched {} product summaries in {}ms", products.size(), duration);
        return products;
    }

    @Override
    public List<ProductSummaryProjection> searchProducts(String keyword) {
        log.info("Searching products with keyword: '{}' (SUMMARY PROJECTION)", keyword);
        return productRepository.findTop5ByNameContainingIgnoreCase(keyword);
    }

    // ============================================================================
    // STATISTICS IMPLEMENTATIONS
    // ============================================================================

    @Override
    public List<ProductRepository.CategoryStatsProjection> getCategoryStatistics() {
        log.info("Fetching category statistics");
        return productRepository.getCategoryStatistics();
    }

    // ============================================================================
    // COMPARISON IMPLEMENTATION
    // ============================================================================

    @Override
    public ComparisonResult comparePerformance() {
        log.info("=== Starting Performance Comparison ===");

        // Test 1: Full Entity
        long fullEntityStart = System.currentTimeMillis();
        List<Product> fullProducts = productRepository.findAll();
        long fullEntityDuration = System.currentTimeMillis() - fullEntityStart;
        int fullEntitySize = calculateSize(fullProducts);

        log.info("FULL ENTITY: {} products in {}ms, estimated size: {}KB",
                fullProducts.size(), fullEntityDuration, fullEntitySize / 1024);

        // Test 2: List Projection
        long projectionStart = System.currentTimeMillis();
        List<ProductListProjection> projectedProducts = productRepository.findAllProjectedBy();
        long projectionDuration = System.currentTimeMillis() - projectionStart;
        int projectionSize = calculateProjectionSize(projectedProducts);

        log.info("LIST PROJECTION: {} products in {}ms, estimated size: {}KB",
                projectedProducts.size(), projectionDuration, projectionSize / 1024);

        // Calculate improvement
        double speedImprovement = ((double) fullEntityDuration / projectionDuration);
        double sizeReduction = ((double) (fullEntitySize - projectionSize) / fullEntitySize) * 100;

        log.info("=== Performance Improvement ===");
        log.info("Speed: {}x faster", String.format("%.2f", speedImprovement));
        log.info("Size: %.1f%% smaller", sizeReduction);

        return new ComparisonResult(
                fullEntityDuration,
                fullEntitySize,
                projectionDuration,
                projectionSize,
                speedImprovement,
                sizeReduction
        );
    }

    // ============================================================================
    // HELPER METHODS
    // ============================================================================

    /**
     * Calculate estimated size of full entities
     *
     * Rough approximation for demonstration:
     * - Average product entity: ~2KB
     * - Includes all 14 fields
     * - Description: 2000 chars
     * - Specifications: 1000 chars
     */
    private int calculateSize(List<Product> products) {
        return products.size() * 2048; // ~2KB per product
    }

    /**
     * Calculate estimated size of projections
     *
     * Rough approximation for demonstration:
     * - Average list projection: ~200 bytes
     * - Only 6 fields (id, name, brand, price, rating, thumbnail)
     * - No heavy text fields
     */
    private int calculateProjectionSize(List<ProductListProjection> products) {
        return products.size() * 200; // ~200 bytes per projection
    }
}