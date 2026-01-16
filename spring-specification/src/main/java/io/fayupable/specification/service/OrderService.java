package io.fayupable.specification.service;

import io.fayupable.specification.entity.Order;
import io.fayupable.specification.enums.OrderStatus;
import io.fayupable.specification.repository.OrderRepository;
import io.fayupable.specification.repository.specification.OrderSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Order Service Implementation
 *
 * Implements business logic with performance comparisons between
 * traditional methods and specification-based methods.
 *
 * Key Implementation Details:
 * - All read operations use @Transactional(readOnly = true)
 * - Comprehensive logging for performance monitoring
 * - Detailed performance comparison methods
 * - Uses Specification.allOf() instead of deprecated where(null)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService implements IOrderService {

    private final OrderRepository orderRepository;

    // ============================================================================
    // TRADITIONAL METHODS IMPLEMENTATION
    // ============================================================================

    @Override
    public List<Order> findAllWithoutSpec() {
        log.info("Finding ALL orders (WITHOUT SPEC) - WARNING: Heavy operation");
        long startTime = System.currentTimeMillis();

        List<Order> orders = orderRepository.findAll();

        long duration = System.currentTimeMillis() - startTime;
        log.info("Loaded {} orders in {}ms (WITHOUT SPEC)", orders.size(), duration);

        return orders;
    }

    @Override
    public List<Order> findByStatusWithoutSpec(OrderStatus status) {
        log.info("Finding orders by status: {} (WITHOUT SPEC)", status);
        long startTime = System.currentTimeMillis();

        List<Order> orders = orderRepository.findByStatus(status);

        long duration = System.currentTimeMillis() - startTime;
        log.info("Found {} orders in {}ms (WITHOUT SPEC)", orders.size(), duration);

        return orders;
    }

    @Override
    public List<Order> findByDateRangeWithoutSpec(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Finding orders by date range: {} to {} (WITHOUT SPEC)", startDate, endDate);
        long startTime = System.currentTimeMillis();

        List<Order> orders = orderRepository.findByCreatedAtBetween(startDate, endDate);

        long duration = System.currentTimeMillis() - startTime;
        log.info("Found {} orders in {}ms (WITHOUT SPEC)", orders.size(), duration);

        return orders;
    }

    @Override
    public List<Order> findComplexWithoutSpec(
            OrderStatus status,
            LocalDateTime startDate,
            LocalDateTime endDate,
            BigDecimal minAmount) {

        log.info("Finding orders with complex filter (WITHOUT SPEC) - status: {}, dates: {} to {}, minAmount: {}",
                status, startDate, endDate, minAmount);

        long startTime = System.currentTimeMillis();

        // PROBLEM: Load ALL orders from database
        List<Order> allOrders = orderRepository.findAll();
        log.info("Loaded {} orders from database", allOrders.size());

        // PROBLEM: Filter in Java (slow!)
        List<Order> filtered = allOrders.stream()
                .filter(order -> status == null || order.getStatus() == status)
                .filter(order -> startDate == null || order.getCreatedAt().isAfter(startDate))
                .filter(order -> endDate == null || order.getCreatedAt().isBefore(endDate))
                .filter(order -> minAmount == null || order.getTotalAmount().compareTo(minAmount) > 0)
                .collect(Collectors.toList());

        long duration = System.currentTimeMillis() - startTime;
        log.info("Filtered to {} orders in {}ms (WITHOUT SPEC) - SLOW!", filtered.size(), duration);

        return filtered;
    }

    // ============================================================================
    // SPECIFICATION METHODS IMPLEMENTATION
    // ============================================================================

    @Override
    public List<Order> findByStatusWithSpec(OrderStatus status) {
        log.info("Finding orders by status: {} (WITH SPEC)", status);
        long startTime = System.currentTimeMillis();

        Specification<Order> spec = OrderSpecification.hasStatus(status);
        List<Order> orders = orderRepository.findAll(spec);

        long duration = System.currentTimeMillis() - startTime;
        log.info("Found {} orders in {}ms (WITH SPEC)", orders.size(), duration);

        return orders;
    }

    @Override
    public List<Order> findByDateRangeWithSpec(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Finding orders by date range: {} to {} (WITH SPEC)", startDate, endDate);
        long startTime = System.currentTimeMillis();

        Specification<Order> spec = OrderSpecification.createdBetween(startDate, endDate);
        List<Order> orders = orderRepository.findAll(spec);

        long duration = System.currentTimeMillis() - startTime;
        log.info("Found {} orders in {}ms (WITH SPEC)", orders.size(), duration);

        return orders;
    }

    @Override
    public List<Order> findComplexWithSpec(
            OrderStatus status,
            LocalDateTime startDate,
            LocalDateTime endDate,
            BigDecimal minAmount) {

        log.info("Finding orders with complex filter (WITH SPEC) - status: {}, dates: {} to {}, minAmount: {}",
                status, startDate, endDate, minAmount);

        long startTime = System.currentTimeMillis();

        // Build dynamic specification using allOf() - NOT deprecated
        List<Specification<Order>> specs = new ArrayList<>();

        if (status != null) {
            specs.add(OrderSpecification.hasStatus(status));
        }
        if (startDate != null || endDate != null) {
            specs.add(OrderSpecification.createdBetween(startDate, endDate));
        }
        if (minAmount != null) {
            specs.add(OrderSpecification.totalAmountGreaterThan(minAmount));
        }

        // Combine all specifications with AND
        Specification<Order> spec = buildSpecification(specs);


        List<Order> orders = orderRepository.findAll(spec);

        long duration = System.currentTimeMillis() - startTime;
        log.info("Found {} orders in {}ms (WITH SPEC) - FAST!", orders.size(), duration);

        return orders;
    }

    @Override
    public List<Order> findAdvancedWithSpec(
            OrderStatus status,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            String customerName,
            String customerCity) {

        log.info("Finding orders with advanced filter (WITH SPEC) - status: {}, amount: {}-{}, customer: {}, city: {}",
                status, minAmount, maxAmount, customerName, customerCity);

        long startTime = System.currentTimeMillis();

        // Build dynamic specification using allOf()
        List<Specification<Order>> specs = new ArrayList<>();

        if (status != null) {
            specs.add(OrderSpecification.hasStatus(status));
        }
        if (minAmount != null || maxAmount != null) {
            specs.add(OrderSpecification.totalAmountBetween(minAmount, maxAmount));
        }
        if (customerName != null && !customerName.trim().isEmpty()) {
            specs.add(OrderSpecification.customerNameContains(customerName));
        }
        if (customerCity != null && !customerCity.trim().isEmpty()) {
            specs.add(OrderSpecification.customerCity(customerCity));
        }

        Specification<Order> spec = buildSpecification(specs);

        List<Order> orders = orderRepository.findAll(spec);

        long duration = System.currentTimeMillis() - startTime;
        log.info("Found {} orders in {}ms (WITH SPEC - ADVANCED)", orders.size(), duration);

        return orders;
    }

    @Override
    public Page<Order> findWithSpecPaginated(
            OrderStatus status,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable) {

        log.info("Finding orders with pagination (WITH SPEC) - page: {}, size: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        List<Specification<Order>> specs = new ArrayList<>();

        if (status != null) {
            specs.add(OrderSpecification.hasStatus(status));
        }
        if (startDate != null || endDate != null) {
            specs.add(OrderSpecification.createdBetween(startDate, endDate));
        }

        Specification<Order> spec = buildSpecification(specs);

        return orderRepository.findAll(spec, pageable);
    }

    @Override
    public List<Order> findActiveOrders() {
        log.info("Finding active orders (WITH SPEC)");

        Specification<Order> spec = OrderSpecification.isActive();
        return orderRepository.findAll(spec);
    }

    @Override
    public List<Order> findHighValueOrders() {
        log.info("Finding high-value orders (WITH SPEC)");

        Specification<Order> spec = OrderSpecification.isHighValue();
        return orderRepository.findAll(spec);
    }

    @Override
    public List<Order> findRecentOrders() {
        log.info("Finding recent orders (WITH SPEC)");

        Specification<Order> spec = OrderSpecification.isRecent();
        return orderRepository.findAll(spec);
    }

    // ============================================================================
    // PERFORMANCE COMPARISON IMPLEMENTATION
    // ============================================================================

    @Override
    public ComparisonResult comparePerformance(
            OrderStatus status,
            LocalDateTime startDate,
            LocalDateTime endDate,
            BigDecimal minAmount) {

        log.info("=== Starting Performance Comparison ===");

        // Test 1: WITHOUT Specification
        long withoutSpecStart = System.currentTimeMillis();
        List<Order> allOrders = orderRepository.findAll();
        int loadedCount = allOrders.size();

        List<Order> filteredWithout = allOrders.stream()
                .filter(order -> status == null || order.getStatus() == status)
                .filter(order -> startDate == null || order.getCreatedAt().isAfter(startDate))
                .filter(order -> endDate == null || order.getCreatedAt().isBefore(endDate))
                .filter(order -> minAmount == null || order.getTotalAmount().compareTo(minAmount) > 0)
                .toList();

        long withoutSpecDuration = System.currentTimeMillis() - withoutSpecStart;
        log.info("WITHOUT SPEC: Loaded {} orders, filtered to {}, took {}ms",
                loadedCount, filteredWithout.size(), withoutSpecDuration);

        // Test 2: WITH Specification
        long withSpecStart = System.currentTimeMillis();

        List<Specification<Order>> specs = new ArrayList<>();

        if (status != null) {
            specs.add(OrderSpecification.hasStatus(status));
        }
        if (startDate != null || endDate != null) {
            specs.add(OrderSpecification.createdBetween(startDate, endDate));
        }
        if (minAmount != null) {
            specs.add(OrderSpecification.totalAmountGreaterThan(minAmount));
        }

        Specification<Order> spec = buildSpecification(specs);

        List<Order> filteredWith = orderRepository.findAll(spec);

        long withSpecDuration = System.currentTimeMillis() - withSpecStart;
        log.info("WITH SPEC: Found {} orders directly, took {}ms",
                filteredWith.size(), withSpecDuration);

        // Calculate improvement
        double speedImprovement = withoutSpecDuration / (double) withSpecDuration;

        log.info("=== Performance Improvement ===");
        log.info("Speed: {}x faster", String.format("%.2f", speedImprovement));
        log.info("Efficiency: Loaded {} vs {} orders", loadedCount, filteredWith.size());

        String description = String.format("%.2fx faster (%.0f%% improvement)",
                speedImprovement,
                ((withoutSpecDuration - withSpecDuration) / (double) withoutSpecDuration) * 100);

        return new ComparisonResult(
                withoutSpecDuration,
                loadedCount,
                filteredWithout.size(),
                withSpecDuration,
                filteredWith.size(),
                speedImprovement,
                description
        );
    }

    private Specification<Order> buildSpecification(List<Specification<Order>> specs) {
        return specs.isEmpty()
                ? (root, query, cb) -> cb.conjunction()
                : Specification.allOf(specs);
    }


    // ============================================================================
    // STATISTICS IMPLEMENTATION
    // ============================================================================

    @Override
    public List<OrderRepository.OrderStatisticsProjection> getOrderStatistics() {
        log.info("Getting order statistics");
        return orderRepository.getOrderStatistics();
    }
}