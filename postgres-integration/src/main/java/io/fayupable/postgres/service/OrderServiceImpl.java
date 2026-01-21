package io.fayupable.postgres.service;

import io.fayupable.postgres.dto.request.CreateOrderItemRequest;
import io.fayupable.postgres.dto.request.CreateOrderRequest;
import io.fayupable.postgres.dto.request.UpdateOrderRequest;
import io.fayupable.postgres.dto.response.OrderResponse;
import io.fayupable.postgres.entity.Order;
import io.fayupable.postgres.entity.OrderItem;
import io.fayupable.postgres.entity.Product;
import io.fayupable.postgres.entity.User;
import io.fayupable.postgres.exception.ResourceNotFoundException;
import io.fayupable.postgres.mapper.OrderMapper;
import io.fayupable.postgres.repository.OrderRepository;
import io.fayupable.postgres.repository.ProductRepository;
import io.fayupable.postgres.repository.UserRepository;
import io.fayupable.postgres.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderMapper orderMapper;

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.debug("Creating order for user id: {}", request.getUserId());

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getUserId()));

        Order order = orderMapper.toEntity(request);
        order.setUser(user);
        order.setOrderNumber(generateOrderNumber());

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CreateOrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + itemRequest.getProductId()));

            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(product.getPrice())
                    .build();

            order.addOrderItem(orderItem);
            totalAmount = totalAmount.add(product.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity())));
        }

        order.setTotalAmount(totalAmount);
        Order savedOrder = orderRepository.save(order);

        log.info("Order created successfully with order number: {}", savedOrder.getOrderNumber());
        return orderMapper.toResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        log.debug("Fetching order by id: {}", id);

        Order order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        log.debug("Fetching all orders with pagination");

        Page<Order> orders = orderRepository.findAll(pageable);
        return orders.map(orderMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersByUserId(Long userId, Pageable pageable) {
        log.debug("Fetching orders for user id: {}", userId);

        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        Page<Order> orders = orderRepository.findByUserId(userId, pageable);
        return orders.map(orderMapper::toResponse);
    }

    @Override
    @Transactional
    public OrderResponse updateOrder(Long id, UpdateOrderRequest request) {
        log.debug("Updating order with id: {}", id);

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

        orderMapper.updateEntityFromRequest(order, request);
        Order updatedOrder = orderRepository.save(order);

        log.info("Order updated successfully with id: {}", updatedOrder.getId());
        return orderMapper.toResponse(updatedOrder);
    }

    @Override
    @Transactional
    public void deleteOrder(Long id) {
        log.debug("Deleting order with id: {}", id);

        if (!orderRepository.existsById(id)) {
            throw new ResourceNotFoundException("Order not found with id: " + id);
        }

        orderRepository.deleteById(id);
        log.info("Order deleted successfully with id: {}", id);
    }

    private String generateOrderNumber() {
        String timestamp = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "ORD-" + timestamp;
    }
}