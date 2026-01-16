package io.fayupable.projection;

import io.fayupable.projection.entity.Product;
import io.fayupable.projection.repository.ProductRepository;
import io.fayupable.projection.repository.projection.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Test
    void findAll_ShouldReturnProducts() {
        List<Product> products = productRepository.findAll();

        assertNotNull(products);
        assertFalse(products.isEmpty());
    }

    @Test
    void findByCategory_ShouldFilterByCategory() {
        List<Product> products = productRepository.findByCategory("Electronics");

        assertNotNull(products);
        assertFalse(products.isEmpty());
        products.forEach(p -> assertEquals("Electronics", p.getCategory()));
    }

    @Test
    void findAllProjectedBy_ShouldReturnProjections() {
        List<ProductListProjection> products = productRepository.findAllProjectedBy();

        assertNotNull(products);
        assertFalse(products.isEmpty());
    }

    @Test
    void findByCategoryPaged_ShouldReturnPage() {
        Page<ProductListProjection> page = productRepository.findByCategory(
                "Electronics",
                PageRequest.of(0, 10)
        );

        assertNotNull(page);
        assertTrue(page.hasContent());
    }

    @Test
    void findProjectedById_ShouldReturnDetailProjection() {
        Optional<ProductDetailProjection> product = productRepository.findProjectedById(1L);

        assertTrue(product.isPresent());
        assertNotNull(product.get().getDescription());
    }

    @Test
    void findByIdIn_ShouldReturnSummaries() {
        List<ProductSummaryProjection> products = productRepository.findByIdIn(
                List.of(1L, 2L, 3L)
        );

        assertNotNull(products);
        assertEquals(3, products.size());
    }

    @Test
    void findByPriceBetween_ShouldFilterByPrice() {
        List<ProductListProjection> products = productRepository.findByPriceBetween(
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(500)
        );

        assertNotNull(products);
        products.forEach(p -> {
            assertTrue(p.getPrice().compareTo(BigDecimal.valueOf(100)) >= 0);
            assertTrue(p.getPrice().compareTo(BigDecimal.valueOf(500)) <= 0);
        });
    }

    @Test
    void getCategoryStatistics_ShouldReturnStats() {
        List<ProductRepository.CategoryStatsProjection> stats =
                productRepository.getCategoryStatistics();

        assertNotNull(stats);
        assertFalse(stats.isEmpty());
    }
}