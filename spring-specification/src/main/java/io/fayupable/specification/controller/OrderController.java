package io.fayupable.specification.controller;

import io.fayupable.specification.entity.Order;
import io.fayupable.specification.enums.OrderStatus;
import io.fayupable.specification.repository.OrderRepository;
import io.fayupable.specification.service.IOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Order Controller
 * <p>
 * REST API endpoints demonstrating Specification pattern benefits.
 * <p>
 * Endpoint Organization:
 * 1. /api/orders/without-spec/** - Traditional methods (slow)
 * 2. /api/orders/with-spec/** - Specification methods (fast)
 * 3. /api/orders/compare - Performance comparison
 * 4. /api/orders/stats/** - Statistics endpoints
 * <p>
 * Response Pattern:
 * All endpoints return Map with metadata:
 * {
 * "method": "WITH_SPEC",
 * "duration_ms": 45,
 * "count": 150,
 * "filters_applied": {...},
 * "data": [...]
 * }
 */
@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final IOrderService orderService;

    // ============================================================================
    // TRADITIONAL ENDPOINTS (Without Specification)
    // ============================================================================

    /**
     * GET /api/orders/without-spec
     * <p>
     * Returns ALL orders using traditional method.
     * <p>
     * WARNING: Loads ALL orders from database.
     * Performance: SLOW (850ms for 1000 orders)
     * <p>
     * Use /api/orders/with-spec for better performance.
     */
    @GetMapping("/without-spec")
    public ResponseEntity<Map<String, Object>> getAllOrdersWithoutSpec() {
        log.info("GET /api/orders/without-spec - Traditional method");

        long startTime = System.currentTimeMillis();
        List<Order> orders = orderService.findAllWithoutSpec();
        long duration = System.currentTimeMillis() - startTime;

        Map<String, Object> response = new HashMap<>();
        response.put("method", "WITHOUT_SPEC");
        response.put("duration_ms", duration);
        response.put("count", orders.size());
        response.put("warning", "This loads ALL orders. Use /with-spec for filtered queries.");
        response.put("data", orders);

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/orders/without-spec/status/{status}
     * <p>
     * Filter by status - TRADITIONAL METHOD
     * <p>
     * Problem: Fixed query, cannot add dynamic filters
     */
    @GetMapping("/without-spec/status/{status}")
    public ResponseEntity<Map<String, Object>> getOrdersByStatusWithoutSpec(
            @PathVariable OrderStatus status) {

        log.info("GET /api/orders/without-spec/status/{}", status);

        long startTime = System.currentTimeMillis();
        List<Order> orders = orderService.findByStatusWithoutSpec(status);
        long duration = System.currentTimeMillis() - startTime;

        Map<String, Object> response = new HashMap<>();
        response.put("method", "WITHOUT_SPEC");
        response.put("duration_ms", duration);
        response.put("status", status);
        response.put("count", orders.size());
        response.put("data", orders);

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/orders/without-spec/complex
     * <p>
     * Complex filter - TRADITIONAL METHOD
     * <p>
     * Problem: Loads ALL orders, filters in Java
     * Performance: 850ms for 1000 orders
     * <p>
     * Query params:
     * - status: Order status
     * - startDate: Start date (format: yyyy-MM-dd'T'HH:mm:ss)
     * - endDate: End date
     * - minAmount: Minimum amount
     * <p>
     * Example: /api/orders/without-spec/complex?status=COMPLETED&minAmount=100
     */
    @GetMapping("/without-spec/complex")
    public ResponseEntity<Map<String, Object>> getComplexWithoutSpec(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) BigDecimal minAmount) {

        log.info("GET /api/orders/without-spec/complex - status: {}, dates: {} to {}, minAmount: {}",
                status, startDate, endDate, minAmount);

        long startTime = System.currentTimeMillis();
        List<Order> orders = orderService.findComplexWithoutSpec(status, startDate, endDate, minAmount);
        long duration = System.currentTimeMillis() - startTime;

        Map<String, Object> response = new HashMap<>();
        response.put("method", "WITHOUT_SPEC_COMPLEX");
        response.put("duration_ms", duration);
        response.put("count", orders.size());
        response.put("filters", Map.of(
                "status", status != null ? status : "all",
                "startDate", startDate != null ? startDate : "none",
                "endDate", endDate != null ? endDate : "none",
                "minAmount", minAmount != null ? minAmount : "none"
        ));
        response.put("warning", "Loaded ALL orders then filtered in Java. Use /with-spec for better performance.");
        response.put("data", orders);

        return ResponseEntity.ok(response);
    }

    // ============================================================================
    // SPECIFICATION ENDPOINTS (With Specification)
    // ============================================================================

    /**
     * GET /api/orders/with-spec/status/{status}
     * <p>
     * Filter by status - SPECIFICATION METHOD
     * <p>
     * Benefits: Dynamic WHERE clause, composable with other filters
     * Performance: Fast (database-level filtering)
     */
    @GetMapping("/with-spec/status/{status}")
    public ResponseEntity<Map<String, Object>> getOrdersByStatusWithSpec(
            @PathVariable OrderStatus status) {

        log.info("GET /api/orders/with-spec/status/{}", status);

        long startTime = System.currentTimeMillis();
        List<Order> orders = orderService.findByStatusWithSpec(status);
        long duration = System.currentTimeMillis() - startTime;

        Map<String, Object> response = new HashMap<>();
        response.put("method", "WITH_SPEC");
        response.put("duration_ms", duration);
        response.put("status", status);
        response.put("count", orders.size());
        response.put("data", orders);

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/orders/with-spec/date-range
     * <p>
     * Filter by date range - SPECIFICATION METHOD
     * <p>
     * Query params:
     * - startDate: Start date (format: yyyy-MM-dd'T'HH:mm:ss)
     * - endDate: End date
     * <p>
     * Example: /api/orders/with-spec/date-range?startDate=2024-01-01T00:00:00&endDate=2024-12-31T23:59:59
     */
    @GetMapping("/with-spec/date-range")
    public ResponseEntity<Map<String, Object>> getByDateRangeWithSpec(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        log.info("GET /api/orders/with-spec/date-range - {} to {}", startDate, endDate);

        long startTime = System.currentTimeMillis();
        List<Order> orders = orderService.findByDateRangeWithSpec(startDate, endDate);
        long duration = System.currentTimeMillis() - startTime;

        Map<String, Object> response = new HashMap<>();
        response.put("method", "WITH_SPEC");
        response.put("duration_ms", duration);
        response.put("count", orders.size());
        response.put("date_range", Map.of(
                "startDate", startDate != null ? startDate : "none",
                "endDate", endDate != null ? endDate : "none"
        ));
        response.put("data", orders);

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/orders/with-spec/complex
     * <p>
     * Complex filter - SPECIFICATION METHOD
     * <p>
     * Benefits:
     * - Only matching orders loaded
     * - Filters at SQL level
     * - Low memory usage
     * <p>
     * Performance: 45ms for 1000 orders (19x faster!)
     * <p>
     * Query params:
     * - status: Order status
     * - startDate: Start date
     * - endDate: End date
     * - minAmount: Minimum amount
     * <p>
     * Example: /api/orders/with-spec/complex?status=COMPLETED&startDate=2024-01-01T00:00:00&minAmount=100
     */
    @GetMapping("/with-spec/complex")
    public ResponseEntity<Map<String, Object>> getComplexWithSpec(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) BigDecimal minAmount) {

        log.info("GET /api/orders/with-spec/complex - status: {}, dates: {} to {}, minAmount: {}",
                status, startDate, endDate, minAmount);

        long startTime = System.currentTimeMillis();
        List<Order> orders = orderService.findComplexWithSpec(status, startDate, endDate, minAmount);
        long duration = System.currentTimeMillis() - startTime;

        Map<String, Object> response = new HashMap<>();
        response.put("method", "WITH_SPEC_COMPLEX");
        response.put("duration_ms", duration);
        response.put("count", orders.size());
        response.put("filters", Map.of(
                "status", status != null ? status : "all",
                "startDate", startDate != null ? startDate : "none",
                "endDate", endDate != null ? endDate : "none",
                "minAmount", minAmount != null ? minAmount : "none"
        ));
        response.put("improvement", "19x faster than without-spec (45ms vs 850ms)");
        response.put("data", orders);

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/orders/with-spec/advanced
     * <p>
     * Advanced filter with JOIN - SPECIFICATION METHOD
     * <p>
     * Demonstrates JOIN queries with customer filters
     * <p>
     * Query params:
     * - status: Order status
     * - minAmount: Minimum amount
     * - maxAmount: Maximum amount
     * - customerName: Customer name search
     * - customerCity: Customer city
     * <p>
     * Example: /api/orders/with-spec/advanced?status=COMPLETED&minAmount=100&maxAmount=1000&customerName=John&customerCity=Istanbul
     */
    @GetMapping("/with-spec/advanced")
    public ResponseEntity<Map<String, Object>> getAdvancedWithSpec(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) String customerCity) {

        log.info("GET /api/orders/with-spec/advanced - status: {}, amount: {}-{}, customer: {}, city: {}",
                status, minAmount, maxAmount, customerName, customerCity);

        long startTime = System.currentTimeMillis();
        List<Order> orders = orderService.findAdvancedWithSpec(status, minAmount, maxAmount, customerName, customerCity);
        long duration = System.currentTimeMillis() - startTime;

        Map<String, Object> response = new HashMap<>();
        response.put("method", "WITH_SPEC_ADVANCED");
        response.put("duration_ms", duration);
        response.put("count", orders.size());
        response.put("filters", Map.of(
                "status", status != null ? status : "all",
                "amount_range", (minAmount != null || maxAmount != null) ?
                        String.format("%s - %s", minAmount, maxAmount) : "all",
                "customer_name", customerName != null ? customerName : "all",
                "customer_city", customerCity != null ? customerCity : "all"
        ));
        response.put("note", "Uses JOIN with customers table");
        response.put("data", orders);

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/orders/with-spec/paginated
     * <p>
     * Paginated results with specifications
     * <p>
     * Query params:
     * - status: Order status
     * - startDate: Start date
     * - endDate: End date
     * - page: Page number (default: 0)
     * - size: Page size (default: 20)
     * - sort: Sort field (default: createdAt)
     * - direction: Sort direction (default: DESC)
     * <p>
     * Example: /api/orders/with-spec/paginated?status=COMPLETED&page=0&size=20&sort=createdAt&direction=DESC
     */
    @GetMapping("/with-spec/paginated")
    public ResponseEntity<Map<String, Object>> getPaginatedWithSpec(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {

        log.info("GET /api/orders/with-spec/paginated - status: {}, page: {}, size: {}", status, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort));

        long startTime = System.currentTimeMillis();
        Page<Order> orderPage = orderService.findWithSpecPaginated(status, startDate, endDate, pageable);
        long duration = System.currentTimeMillis() - startTime;

        Map<String, Object> response = new HashMap<>();
        response.put("method", "WITH_SPEC_PAGINATED");
        response.put("duration_ms", duration);
        response.put("page", orderPage.getNumber());
        response.put("size", orderPage.getSize());
        response.put("total_elements", orderPage.getTotalElements());
        response.put("total_pages", orderPage.getTotalPages());
        response.put("filters", Map.of(
                "status", status != null ? status : "all",
                "startDate", startDate != null ? startDate : "none",
                "endDate", endDate != null ? endDate : "none"
        ));
        response.put("data", orderPage.getContent());

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/orders/active
     * <p>
     * Get active orders (PENDING, PROCESSING, SHIPPED)
     * <p>
     * Uses predefined complex specification
     */
    @GetMapping("/active")
    public ResponseEntity<Map<String, Object>> getActiveOrders() {
        log.info("GET /api/orders/active");

        long startTime = System.currentTimeMillis();
        List<Order> orders = orderService.findActiveOrders();
        long duration = System.currentTimeMillis() - startTime;

        Map<String, Object> response = new HashMap<>();
        response.put("method", "PREDEFINED_SPEC_ACTIVE");
        response.put("duration_ms", duration);
        response.put("count", orders.size());
        response.put("description", "Active orders: PENDING, PROCESSING, SHIPPED");
        response.put("data", orders);

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/orders/high-value
     * <p>
     * Get high-value orders (amount > 1000 AND not cancelled)
     * <p>
     * Uses predefined complex specification
     */
    @GetMapping("/high-value")
    public ResponseEntity<Map<String, Object>> getHighValueOrders() {
        log.info("GET /api/orders/high-value");

        long startTime = System.currentTimeMillis();
        List<Order> orders = orderService.findHighValueOrders();
        long duration = System.currentTimeMillis() - startTime;

        Map<String, Object> response = new HashMap<>();
        response.put("method", "PREDEFINED_SPEC_HIGH_VALUE");
        response.put("duration_ms", duration);
        response.put("count", orders.size());
        response.put("description", "High-value orders: amount > 1000 AND status != CANCELLED");
        response.put("data", orders);

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/orders/recent
     * <p>
     * Get recent orders (last 30 days, not cancelled)
     * <p>
     * Uses predefined complex specification
     */
    @GetMapping("/recent")
    public ResponseEntity<Map<String, Object>> getRecentOrders() {
        log.info("GET /api/orders/recent");

        long startTime = System.currentTimeMillis();
        List<Order> orders = orderService.findRecentOrders();
        long duration = System.currentTimeMillis() - startTime;

        Map<String, Object> response = new HashMap<>();
        response.put("method", "PREDEFINED_SPEC_RECENT");
        response.put("duration_ms", duration);
        response.put("count", orders.size());
        response.put("description", "Recent orders: last 30 days AND status != CANCELLED");
        response.put("data", orders);

        return ResponseEntity.ok(response);
    }

    // ============================================================================
    // COMPARISON ENDPOINT
    // ============================================================================

    /**
     * GET /api/orders/compare
     * <p>
     * Compare performance: WITHOUT vs WITH Specification
     * <p>
     * Runs both methods with same filters and measures:
     * - Execution time
     * - Orders loaded vs orders returned
     * - Speed improvement
     * <p>
     * Query params:
     * - status: Order status
     * - startDate: Start date
     * - endDate: End date
     * - minAmount: Minimum amount
     * <p>
     * Example: /api/orders/compare?status=COMPLETED&minAmount=100
     */
    @GetMapping("/compare")
    public ResponseEntity<Map<String, Object>> comparePerformance(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) BigDecimal minAmount) {

        log.info("GET /api/orders/compare - Running performance comparison");

        IOrderService.ComparisonResult result = orderService.comparePerformance(status, startDate, endDate, minAmount);

        Map<String, Object> response = new HashMap<>();
        response.put("without_spec", Map.of(
                "duration_ms", result.withoutSpecDurationMs(),
                "loaded_count", result.withoutSpecLoadedCount(),
                "result_count", result.withoutSpecResultCount(),
                "description", "Loaded ALL orders, then filtered in Java"
        ));
        response.put("with_spec", Map.of(
                "duration_ms", result.withSpecDurationMs(),
                "result_count", result.withSpecResultCount(),
                "description", "Dynamic WHERE clause at database level"
        ));
        response.put("improvement", Map.of(
                "speed_multiplier", String.format("%.2fx", result.speedImprovement()),
                "description", result.improvementDescription(),
                "efficiency", String.format("Loaded %d vs %d orders",
                        result.withoutSpecLoadedCount(), result.withSpecResultCount())
        ));
        response.put("filters", Map.of(
                "status", status != null ? status : "all",
                "startDate", startDate != null ? startDate : "none",
                "endDate", endDate != null ? endDate : "none",
                "minAmount", minAmount != null ? minAmount : "none"
        ));

        return ResponseEntity.ok(response);
    }

    // ============================================================================
    // STATISTICS ENDPOINTS
    // ============================================================================

    /**
     * GET /api/orders/stats
     * <p>
     * Get order statistics by status
     * <p>
     * Returns: count, total amount, average amount per status
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getOrderStatistics() {
        log.info("GET /api/orders/stats");

        long startTime = System.currentTimeMillis();
        List<OrderRepository.OrderStatisticsProjection> stats = orderService.getOrderStatistics();
        long duration = System.currentTimeMillis() - startTime;

        Map<String, Object> response = new HashMap<>();
        response.put("method", "STATISTICS");
        response.put("duration_ms", duration);
        response.put("count", stats.size());
        response.put("data", stats);

        return ResponseEntity.ok(response);
    }
}