package io.fayupable.postgres.service;

import io.fayupable.postgres.dto.request.CreateOrderRequest;
import io.fayupable.postgres.dto.request.UpdateOrderRequest;
import io.fayupable.postgres.dto.response.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {

    OrderResponse createOrder(CreateOrderRequest request);

    OrderResponse getOrderById(Long id);

    Page<OrderResponse> getAllOrders(Pageable pageable);

    Page<OrderResponse> getOrdersByUserId(Long userId, Pageable pageable);

    OrderResponse updateOrder(Long id, UpdateOrderRequest request);

    void deleteOrder(Long id);
}