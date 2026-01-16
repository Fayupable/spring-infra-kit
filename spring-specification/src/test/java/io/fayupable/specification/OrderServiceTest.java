package io.fayupable.specification;

import io.fayupable.specification.entity.Order;
import io.fayupable.specification.enums.OrderStatus;
import io.fayupable.specification.repository.OrderRepository;
import io.fayupable.specification.service.IOrderService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Slf4j
class OrderServiceTest {

    @Autowired
    private IOrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void findAllWithoutSpec_ShouldReturnAllOrders() {
        List<Order> orders = orderService.findAllWithoutSpec();

        assertNotNull(orders);
        assertFalse(orders.isEmpty());
        assertTrue(orders.size() >= 1000);
    }

    @Test
    void findByStatusWithoutSpec_ShouldFilterByStatus() {
        List<Order> orders = orderService.findByStatusWithoutSpec(OrderStatus.COMPLETED);

        assertNotNull(orders);
        orders.forEach(order -> assertEquals(OrderStatus.COMPLETED, order.getStatus()));
    }

    @Test
    void findByDateRangeWithoutSpec_ShouldFilterByDateRange() {
        LocalDateTime startDate = LocalDateTime.now().minusDays(30);
        LocalDateTime endDate = LocalDateTime.now();

        List<Order> orders = orderService.findByDateRangeWithoutSpec(startDate, endDate);

        assertNotNull(orders);
        orders.forEach(order -> {
            assertTrue(order.getCreatedAt().isAfter(startDate));
            assertTrue(order.getCreatedAt().isBefore(endDate));
        });
    }

    @Test
    void findComplexWithoutSpec_ShouldFilterInJava() {
        LocalDateTime startDate = LocalDateTime.now().minusDays(60);
        LocalDateTime endDate = LocalDateTime.now();
        BigDecimal minAmount = BigDecimal.valueOf(100);

        List<Order> orders = orderService.findComplexWithoutSpec(
                OrderStatus.COMPLETED,
                startDate,
                endDate,
                minAmount
        );

        assertNotNull(orders);
        orders.forEach(order -> {
            assertEquals(OrderStatus.COMPLETED, order.getStatus());
            assertTrue(order.getCreatedAt().isAfter(startDate));
            assertTrue(order.getCreatedAt().isBefore(endDate));
            assertTrue(order.getTotalAmount().compareTo(minAmount) > 0);
        });
    }

    @Test
    void findByStatusWithSpec_ShouldFilterAtDatabase() {
        List<Order> orders = orderService.findByStatusWithSpec(OrderStatus.PENDING);

        assertNotNull(orders);
        assertFalse(orders.isEmpty());
        orders.forEach(order -> assertEquals(OrderStatus.PENDING, order.getStatus()));
    }

    @Test
    void findByDateRangeWithSpec_ShouldFilterAtDatabase() {
        LocalDateTime startDate = LocalDateTime.now().minusDays(30);
        LocalDateTime endDate = LocalDateTime.now();

        List<Order> orders = orderService.findByDateRangeWithSpec(startDate, endDate);

        assertNotNull(orders);
        orders.forEach(order -> {
            assertTrue(order.getCreatedAt().isAfter(startDate));
            assertTrue(order.getCreatedAt().isBefore(endDate));
        });
    }

    @Test
    void findComplexWithSpec_ShouldFilterAtDatabase() {
        LocalDateTime startDate = LocalDateTime.now().minusDays(60);
        LocalDateTime endDate = LocalDateTime.now();
        BigDecimal minAmount = BigDecimal.valueOf(100);

        List<Order> orders = orderService.findComplexWithSpec(
                OrderStatus.SHIPPED,
                startDate,
                endDate,
                minAmount
        );

        assertNotNull(orders);
        orders.forEach(order -> {
            assertEquals(OrderStatus.SHIPPED, order.getStatus());
            assertTrue(order.getCreatedAt().isAfter(startDate));
            assertTrue(order.getCreatedAt().isBefore(endDate));
            assertTrue(order.getTotalAmount().compareTo(minAmount) > 0);
        });
    }

    @Test
    void findAdvancedWithSpec_ShouldFilterWithJoin() {
        List<Order> orders = orderService.findAdvancedWithSpec(
                OrderStatus.DELIVERED,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(1000),
                "Ahmet",
                "Istanbul"
        );

        assertNotNull(orders);
        orders.forEach(order -> {
            assertEquals(OrderStatus.DELIVERED, order.getStatus());
            assertTrue(order.getTotalAmount().compareTo(BigDecimal.valueOf(100)) >= 0);
            assertTrue(order.getTotalAmount().compareTo(BigDecimal.valueOf(1000)) <= 0);
        });
    }

    @Test
    void findWithSpecPaginated_ShouldReturnPagedResults() {
        Page<Order> page = orderService.findWithSpecPaginated(
                OrderStatus.PROCESSING,
                null,
                null,
                PageRequest.of(0, 10)
        );

        assertNotNull(page);
        assertTrue(page.hasContent());
        assertEquals(10, page.getSize());
        page.getContent().forEach(order ->
                assertEquals(OrderStatus.PROCESSING, order.getStatus())
        );
    }

    @Test
    void findActiveOrders_ShouldReturnActiveStatuses() {
        List<Order> orders = orderService.findActiveOrders();

        assertNotNull(orders);
        assertFalse(orders.isEmpty());

        orders.forEach(order -> {
            OrderStatus status = order.getStatus();
            assertTrue(
                    status == OrderStatus.PENDING ||
                            status == OrderStatus.PROCESSING ||
                            status == OrderStatus.SHIPPED
            );
        });
    }

    @Test
    void findHighValueOrders_ShouldReturnHighValueOrders() {
        List<Order> orders = orderService.findHighValueOrders();

        assertNotNull(orders);

        orders.forEach(order -> {
            assertTrue(order.getTotalAmount().compareTo(BigDecimal.valueOf(1000)) > 0);
            assertNotEquals(OrderStatus.CANCELLED, order.getStatus());
        });
    }

    @Test
    void findRecentOrders_ShouldReturnRecentOrders() {
        List<Order> orders = orderService.findRecentOrders();

        assertNotNull(orders);

        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        orders.forEach(order -> {
            assertTrue(order.getCreatedAt().isAfter(thirtyDaysAgo));
            assertNotEquals(OrderStatus.CANCELLED, order.getStatus());
        });
    }

    @Test
    void comparePerformance_ShouldShowImprovement() {
        LocalDateTime startDate = LocalDateTime.now().minusDays(60);
        LocalDateTime endDate = LocalDateTime.now();
        BigDecimal minAmount = BigDecimal.valueOf(100);

        IOrderService.ComparisonResult result = orderService.comparePerformance(
                OrderStatus.COMPLETED,
                startDate,
                endDate,
                minAmount
        );

        assertNotNull(result);
        assertTrue(result.withoutSpecDurationMs() > 0);
        assertTrue(result.withSpecDurationMs() > 0);
        assertTrue(result.speedImprovement() > 0);

        assertEquals(result.withoutSpecResultCount(), result.withSpecResultCount());

        assertTrue(result.withoutSpecLoadedCount() > result.withSpecResultCount());

        log.info("Performance comparison: {}", result.improvementDescription());
    }

    @Test
    void getOrderStatistics_ShouldReturnStats() {
        List<OrderRepository.OrderStatisticsProjection> stats = orderService.getOrderStatistics();

        assertNotNull(stats);
        assertFalse(stats.isEmpty());

        stats.forEach(stat -> {
            assertNotNull(stat.getStatus());
            assertNotNull(stat.getOrderCount());
            assertTrue(stat.getOrderCount() > 0);
        });
    }

    @Test
    void specificationPerformance_ComplexFilterShouldBeFaster() {
        LocalDateTime startDate = LocalDateTime.now().minusDays(90);
        LocalDateTime endDate = LocalDateTime.now();
        BigDecimal minAmount = BigDecimal.valueOf(200);

        long withoutSpecStart = System.currentTimeMillis();
        List<Order> withoutSpec = orderService.findComplexWithoutSpec(
                OrderStatus.DELIVERED,
                startDate,
                endDate,
                minAmount
        );
        long withoutSpecDuration = System.currentTimeMillis() - withoutSpecStart;

        long withSpecStart = System.currentTimeMillis();
        List<Order> withSpec = orderService.findComplexWithSpec(
                OrderStatus.DELIVERED,
                startDate,
                endDate,
                minAmount
        );
        long withSpecDuration = System.currentTimeMillis() - withSpecStart;

        assertEquals(withoutSpec.size(), withSpec.size());

        assertTrue(withSpecDuration < withoutSpecDuration,
                String.format("Spec should be faster: %dms vs %dms",
                        withSpecDuration, withoutSpecDuration));

        double improvement = (double) withoutSpecDuration / withSpecDuration;
        log.info("Specification is {}x faster ({} ms vs {} ms)",
                String.format("%.2f", improvement),
                withSpecDuration,
                withoutSpecDuration);
    }
}