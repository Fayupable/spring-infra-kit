package io.fayupable.projection.controller;

import io.fayupable.projection.entity.Product;
import io.fayupable.projection.repository.projection.*;
import io.fayupable.projection.repository.ProductRepository;
import io.fayupable.projection.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Product Controller
 * <p>
 * REST API endpoints demonstrating projection patterns.
 * <p>
 * Endpoint Organization:
 * 1. /api/products/full/** - Full entity endpoints (heavy)
 * 2. /api/products/list/** - List projection endpoints (light)
 * 3. /api/products/detail/** - Detail projection endpoints (medium)
 * 4. /api/products/summary/** - Summary projection endpoints (ultra-light)
 * 5. /api/products/stats/** - Statistics endpoints
 * 6. /api/products/compare - Performance comparison
 * <p>
 * Response Pattern:
 * All endpoints return Map with metadata for demonstration:
 * {
 * "method": "LIST_PROJECTION",
 * "duration_ms": 15,
 * "count": 200,
 * "estimated_size_kb": 40,
 * "data": [...]
 * }
 * <p>
 * This helps visualize performance differences.
 */
@Slf4j
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // ============================================================================
    // FULL ENTITY ENDPOINTS (Heavy - Not Recommended for Lists)
    // ============================================================================

    /**
     * GET /api/products/full
     * <p>
     * Returns ALL products as full entities.
     * <p>
     * WARNING: This is HEAVY and SLOW.
     * - Fetches all 14 fields per product
     * - 200 products x 2KB = 400KB response
     * - Use ONLY when you need to UPDATE products
     * <p>
     * For READ-ONLY list views, use /api/products/list instead.
     * <p>
     * Example Response:
     * {
     * "method": "FULL_ENTITY",
     * "duration_ms": 145,
     * "count": 200,
     * "estimated_size_kb": 400,
     * "warning": "This endpoint fetches ALL fields. Use /list for better performance.",
     * "data": [full Product objects]
     * }
     */
    @GetMapping("/full")
    public ResponseEntity<Map<String, Object>> getAllProductsFull() {
        log.info("GET /api/products/full - Fetching FULL entities");

        long startTime = System.currentTimeMillis();
        List<Product> products = productService.getAllProducts();
        long duration = System.currentTimeMillis() - startTime;

        Map<String, Object> response = new HashMap<>();
        response.put("method", "FULL_ENTITY");
        response.put("duration_ms", duration);
        response.put("count", products.size());
        response.put("estimated_size_kb", products.size() * 2); // ~2KB per product
        response.put("warning", "This endpoint fetches ALL fields. Use /list for better performance.");
        response.put("data", products);

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/products/full/{id}
     * <p>
     * Returns single product as full entity.
     * <p>
     * Use when you need to UPDATE the product.
     * For READ-ONLY detail view, use /api/products/detail/{id} instead.
     */
    @GetMapping("/full/{id}")
    public ResponseEntity<Map<String, Object>> getProductByIdFull(@PathVariable Long id) {
        log.info("GET /api/products/full/{} - Fetching FULL entity", id);

        long startTime = System.currentTimeMillis();
        Product product = productService.getProductById(id);
        long duration = System.currentTimeMillis() - startTime;

        Map<String, Object> response = new HashMap<>();
        response.put("method", "FULL_ENTITY");
        response.put("duration_ms", duration);
        response.put("note", "Use /detail/{id} for read-only views (25% faster)");
        response.put("data", product);

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/products/full/category/{category}
     * <p>
     * Returns products by category as full entities.
     * <p>
     * Use when you need to modify products after fetching.
     * For display-only, use /api/products/list/category/{category} instead.
     */
    @GetMapping("/full/category/{category}")
    public ResponseEntity<Map<String, Object>> getProductsByCategoryFull(@PathVariable String category) {
        log.info("GET /api/products/full/category/{} - Fetching FULL entities", category);

        long startTime = System.currentTimeMillis();
        List<Product> products = productService.getProductsByCategory(category);
        long duration = System.currentTimeMillis() - startTime;

        Map<String, Object> response = new HashMap<>();
        response.put("method", "FULL_ENTITY");
        response.put("duration_ms", duration);
        response.put("count", products.size());
        response.put("category", category);
        response.put("data", products);

        return ResponseEntity.ok(response);
    }

    // ============================================================================
    // LIST PROJECTION ENDPOINTS (Light - Recommended for Lists)
    // ============================================================================

    /**
     * GET /api/products/list
     * <p>
     * Returns ALL products as list projections.
     * <p>
     * RECOMMENDED for catalog pages, search results.
     * - Fetches only 6 fields (id, name, brand, price, rating, thumbnail)
     * - 200 products x 200 bytes = 40KB response
     * - 90% faster than /full endpoint
     * <p>
     * Example Response:
     * {
     * "method": "LIST_PROJECTION",
     * "duration_ms": 15,
     * "count": 200,
     * "estimated_size_kb": 40,
     * "improvement": "90% smaller and faster than /full",
     * "data": [list projections]
     * }
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getAllProductsList() {
        log.info("GET /api/products/list - Fetching LIST projections");

        long startTime = System.currentTimeMillis();
        List<ProductListProjection> products = productService.getAllProductsList();
        long duration = System.currentTimeMillis() - startTime;

        Map<String, Object> response = new HashMap<>();
        response.put("method", "LIST_PROJECTION");
        response.put("duration_ms", duration);
        response.put("count", products.size());
        response.put("estimated_size_kb", (products.size() * 200) / 1024); // ~200 bytes per projection
        response.put("improvement", "90% smaller and faster than /full");
        response.put("data", products);

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/products/list/category/{category}
     * <p>
     * Returns products by category with pagination.
     * <p>
     * Query params:
     * - page: Page number (default: 0)
     * - size: Page size (default: 20)
     * - sort: Sort field (default: name)
     * <p>
     * Example: /api/products/list/category/Electronics?page=0&size=20&sort=price
     */
    @GetMapping("/list/category/{category}")
    public ResponseEntity<Map<String, Object>> getProductsByCategoryList(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sort) {

        log.info("GET /api/products/list/category/{} - page: {}, size: {}", category, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(sort));

        long startTime = System.currentTimeMillis();
        Page<ProductListProjection> productsPage = productService.getProductsByCategoryPaged(category, pageable);
        long duration = System.currentTimeMillis() - startTime;

        Map<String, Object> response = new HashMap<>();
        response.put("method", "LIST_PROJECTION_PAGED");
        response.put("duration_ms", duration);
        response.put("category", category);
        response.put("page", productsPage.getNumber());
        response.put("size", productsPage.getSize());
        response.put("total_elements", productsPage.getTotalElements());
        response.put("total_pages", productsPage.getTotalPages());
        response.put("data", productsPage.getContent());

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/products/list/brand/{brand}
     * <p>
     * Returns products by brand, sorted by rating (highest first).
     * <p>
     * Example: /api/products/list/brand/Apple
     */
    @GetMapping("/list/brand/{brand}")
    public ResponseEntity<Map<String, Object>> getProductsByBrandList(@PathVariable String brand) {
        log.info("GET /api/products/list/brand/{}", brand);

        long startTime = System.currentTimeMillis();
        List<ProductListProjection> products = productService.getProductsByBrand(brand);
        long duration = System.currentTimeMillis() - startTime;

        Map<String, Object> response = new HashMap<>();
        response.put("method", "LIST_PROJECTION");
        response.put("duration_ms", duration);
        response.put("brand", brand);
        response.put("count", products.size());
        response.put("sorted_by", "rating DESC");
        response.put("data", products);

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/products/list/price-range
     * <p>
     * Returns products in price range.
     * <p>
     * Query params:
     * - min: Minimum price (required)
     * - max: Maximum price (required)
     * <p>
     * Example: /api/products/list/price-range?min=100&max=500
     */
    @GetMapping("/list/price-range")
    public ResponseEntity<Map<String, Object>> getProductsByPriceRangeList(
            @RequestParam BigDecimal min,
            @RequestParam BigDecimal max) {

        log.info("GET /api/products/list/price-range - min: ${}, max: ${}", min, max);

        long startTime = System.currentTimeMillis();
        List<ProductListProjection> products = productService.getProductsByPriceRange(min, max);
        long duration = System.currentTimeMillis() - startTime;

        Map<String, Object> response = new HashMap<>();
        response.put("method", "LIST_PROJECTION");
        response.put("duration_ms", duration);
        response.put("price_range", String.format("$%.2f - $%.2f", min, max));
        response.put("count", products.size());
        response.put("data", products);

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/products/list/top-rated
     * <p>
     * Returns top 10 highest-rated products (rating >= 4.5).
     * <p>
     * Use case: "Top Rated" section on homepage
     */
    @GetMapping("/list/top-rated")
    public ResponseEntity<Map<String, Object>> getTopRatedProductsList() {
        log.info("GET /api/products/list/top-rated");

        long startTime = System.currentTimeMillis();
        List<ProductListProjection> products = productService.getTopRatedProducts();
        long duration = System.currentTimeMillis() - startTime;

        Map<String, Object> response = new HashMap<>();
        response.put("method", "LIST_PROJECTION");
        response.put("duration_ms", duration);
        response.put("min_rating", 4.5);
        response.put("count", products.size());
        response.put("data", products);

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/products/list/filter
     * <p>
     * Returns filtered products with multiple criteria.
     * <p>
     * Query params:
     * - category: Category name (required)
     * - brand: Brand name (required)
     * - minPrice: Minimum price (required)
     * - maxPrice: Maximum price (required)
     * <p>
     * Example: /api/products/list/filter?category=Electronics&brand=Apple&minPrice=500&maxPrice=2000
     */
    @GetMapping("/list/filter")
    public ResponseEntity<Map<String, Object>> filterProductsList(
            @RequestParam String category,
            @RequestParam String brand,
            @RequestParam BigDecimal minPrice,
            @RequestParam BigDecimal maxPrice) {

        log.info("GET /api/products/list/filter - category: {}, brand: {}, price: ${}-${}",
                category, brand, minPrice, maxPrice);

        long startTime = System.currentTimeMillis();
        List<ProductListProjection> products = productService.filterProducts(category, brand, minPrice, maxPrice);
        long duration = System.currentTimeMillis() - startTime;

        Map<String, Object> response = new HashMap<>();
        response.put("method", "LIST_PROJECTION_FILTERED");
        response.put("duration_ms", duration);
        response.put("filters", Map.of(
                "category", category,
                "brand", brand,
                "price_range", String.format("$%.2f - $%.2f", minPrice, maxPrice)
        ));
        response.put("count", products.size());
        response.put("data", products);

        return ResponseEntity.ok(response);
    }

    // ============================================================================
    // DETAIL PROJECTION ENDPOINTS (Medium - For Detail Pages)
    // ============================================================================

    /**
     * GET /api/products/detail/{id}
     * <p>
     * Returns single product as detail projection.
     * <p>
     * RECOMMENDED for product detail pages.
     * - Includes description and specifications
     * - Excludes analytics fields (viewCount, salesCount, timestamps)
     * - 25% faster than /full/{id}
     * <p>
     * Example Response:
     * {
     * "method": "DETAIL_PROJECTION",
     * "duration_ms": 8,
     * "improvement": "25% faster than /full",
     * "data": {detail projection}
     * }
     */
    @GetMapping("/detail/{id}")
    public ResponseEntity<Map<String, Object>> getProductDetail(@PathVariable Long id) {
        log.info("GET /api/products/detail/{} - Fetching DETAIL projection", id);

        long startTime = System.currentTimeMillis();
        ProductDetailProjection product = productService.getProductDetail(id);
        long duration = System.currentTimeMillis() - startTime;

        Map<String, Object> response = new HashMap<>();
        response.put("method", "DETAIL_PROJECTION");
        response.put("duration_ms", duration);
        response.put("improvement", "25% faster than /full");
        response.put("data", product);

        return ResponseEntity.ok(response);
    }

    // ============================================================================
    // SUMMARY PROJECTION ENDPOINTS (Ultra-Light - For Minimal Views)
    // ============================================================================

    /**
     * GET /api/products/summary
     * <p>
     * Returns products by IDs as summary projections.
     * <p>
     * RECOMMENDED for shopping cart, order history.
     * - Only 3 fields: id, name, price
     * - 97.5% smaller than full entities
     * - 40x faster
     * <p>
     * Query params:
     * - ids: Comma-separated product IDs
     * <p>
     * Example: /api/products/summary?ids=1,2,3,4,5
     * <p>
     * Example Response:
     * {
     * "method": "SUMMARY_PROJECTION",
     * "duration_ms": 3,
     * "count": 5,
     * "estimated_size_kb": 0.25,
     * "improvement": "97.5% smaller than full entities",
     * "data": [minimal projections]
     * }
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getProductsSummary(@RequestParam List<Long> ids) {
        log.info("GET /api/products/summary - {} products", ids.size());

        long startTime = System.currentTimeMillis();
        List<ProductSummaryProjection> products = productService.getProductsSummary(ids);
        long duration = System.currentTimeMillis() - startTime;

        Map<String, Object> response = new HashMap<>();
        response.put("method", "SUMMARY_PROJECTION");
        response.put("duration_ms", duration);
        response.put("count", products.size());
        response.put("estimated_size_kb", (products.size() * 50) / 1024.0); // ~50 bytes per summary
        response.put("improvement", "97.5% smaller than full entities");
        response.put("data", products);

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/products/search
     * <p>
     * Quick product search for autocomplete.
     * <p>
     * Returns top 5 matching products with minimal data.
     * <p>
     * Query params:
     * - q: Search keyword
     * <p>
     * Example: /api/products/search?q=iPhone
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchProducts(@RequestParam String q) {
        log.info("GET /api/products/search?q={}", q);

        long startTime = System.currentTimeMillis();
        List<ProductSummaryProjection> products = productService.searchProducts(q);
        long duration = System.currentTimeMillis() - startTime;

        Map<String, Object> response = new HashMap<>();
        response.put("method", "SUMMARY_PROJECTION_SEARCH");
        response.put("duration_ms", duration);
        response.put("keyword", q);
        response.put("count", products.size());
        response.put("max_results", 5);
        response.put("data", products);

        return ResponseEntity.ok(response);
    }

    // ============================================================================
    // STATISTICS ENDPOINTS
    // ============================================================================

    /**
     * GET /api/products/stats/categories
     * <p>
     * Returns aggregate statistics by category.
     * <p>
     * Use case: Admin dashboard analytics
     * <p>
     * Example Response:
     * {
     * "method": "AGGREGATE_PROJECTION",
     * "data": [
     * {
     * "category": "Electronics",
     * "productCount": 45,
     * "averagePrice": 850.50,
     * "averageRating": 4.3
     * },
     * ...
     * ]
     * }
     */
    @GetMapping("/stats/categories")
    public ResponseEntity<Map<String, Object>> getCategoryStatistics() {
        log.info("GET /api/products/stats/categories");

        long startTime = System.currentTimeMillis();
        List<ProductRepository.CategoryStatsProjection> stats = productService.getCategoryStatistics();
        long duration = System.currentTimeMillis() - startTime;

        Map<String, Object> response = new HashMap<>();
        response.put("method", "AGGREGATE_PROJECTION");
        response.put("duration_ms", duration);
        response.put("count", stats.size());
        response.put("data", stats);

        return ResponseEntity.ok(response);
    }

    // ============================================================================
    // PERFORMANCE COMPARISON ENDPOINT
    // ============================================================================

    /**
     * GET /api/products/compare
     * <p>
     * Compares performance between FULL ENTITY and PROJECTION queries.
     * <p>
     * Executes both methods and returns timing + size metrics.
     * <p>
     * Example Response:
     * {
     * "full_entity": {
     * "duration_ms": 145,
     * "size_kb": 400
     * },
     * "list_projection": {
     * "duration_ms": 15,
     * "size_kb": 40
     * },
     * "improvement": {
     * "speed": "9.67x faster",
     * "size": "90% smaller"
     * }
     * }
     */
    @GetMapping("/compare")
    public ResponseEntity<Map<String, Object>> comparePerformance() {
        log.info("GET /api/products/compare - Running performance comparison");

        ProductService.ComparisonResult result = productService.comparePerformance();

        Map<String, Object> response = new HashMap<>();
        response.put("full_entity", Map.of(
                "duration_ms", result.fullEntityDurationMs(),
                "size_kb", result.fullEntitySizeBytes() / 1024
        ));
        response.put("list_projection", Map.of(
                "duration_ms", result.projectionDurationMs(),
                "size_kb", result.projectionSizeBytes() / 1024
        ));
        response.put("improvement", Map.of(
                "speed", String.format("%.2fx faster", result.speedImprovement()),
                "size", String.format("%.1f%% smaller", result.sizeReductionPercent())
        ));

        return ResponseEntity.ok(response);
    }
}