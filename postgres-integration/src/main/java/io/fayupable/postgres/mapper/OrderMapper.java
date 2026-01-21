package io.fayupable.postgres.mapper;

import io.fayupable.postgres.dto.request.CreateOrderRequest;
import io.fayupable.postgres.dto.request.UpdateOrderRequest;
import io.fayupable.postgres.dto.response.OrderItemResponse;
import io.fayupable.postgres.dto.response.OrderResponse;
import io.fayupable.postgres.entity.Order;
import io.fayupable.postgres.entity.OrderItem;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class OrderMapper {

    public Order toEntity(CreateOrderRequest request) {
        if (request == null) {
            return null;
        }

        return Order.builder()
                .notes(request.getNotes())
                .status("PENDING")
                .build();
    }

    public OrderResponse toResponse(Order order) {
        if (order == null) {
            return null;
        }

        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUser() != null ? order.getUser().getId() : null)
                .orderNumber(order.getOrderNumber())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .notes(order.getNotes())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .items(toOrderItemResponseList(order.getOrderItems()))
                .build();
    }

    public OrderItemResponse toOrderItemResponse(OrderItem orderItem) {
        if (orderItem == null) {
            return null;
        }

        return OrderItemResponse.builder()
                .id(orderItem.getId())
                .productId(orderItem.getProduct() != null ? orderItem.getProduct().getId() : null)
                .productName(orderItem.getProduct() != null ? orderItem.getProduct().getName() : null)
                .quantity(orderItem.getQuantity())
                .unitPrice(orderItem.getUnitPrice())
                .totalPrice(orderItem.getTotalPrice())
                .createdAt(orderItem.getCreatedAt())
                .build();
    }

    public List<OrderItemResponse> toOrderItemResponseList(List<OrderItem> orderItems) {
        if (orderItems == null) {
            return List.of();
        }

        return orderItems.stream()
                .filter(Objects::nonNull)
                .map(this::toOrderItemResponse)
                .collect(Collectors.toList());
    }

    public List<OrderResponse> toResponseList(List<Order> orders) {
        if (orders == null) {
            return List.of();
        }

        return orders.stream()
                .filter(Objects::nonNull)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public void updateEntityFromRequest(Order order, UpdateOrderRequest request) {
        if (order == null || request == null) {
            return;
        }

        if (request.getStatus() != null) {
            order.setStatus(request.getStatus());
        }
        if (request.getNotes() != null) {
            order.setNotes(request.getNotes());
        }
    }
}