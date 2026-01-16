package io.fayupable.specification.repository;

import io.fayupable.specification.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Order Item Repository
 * <p>
 * Basic CRUD operations for OrderItem entity.
 * Not heavily used in specification demo.
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}