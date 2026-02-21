package com.eventflow.orderservice.dto;

import com.eventflow.orderservice.model.Order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String customerEmail,
        String productName,
        Integer quantity,
        BigDecimal totalPrice,
        String status,
        Instant createdAt
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getCustomerEmail(),
                order.getProductName(),
                order.getQuantity(),
                order.getTotalPrice(),
                order.getStatus().name(),
                order.getCreatedAt()
        );
    }
}
