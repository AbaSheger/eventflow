package com.eventflow.orderservice.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderPlacedEvent(
        UUID orderId,
        String customerEmail,
        String productName,
        Integer quantity,
        BigDecimal totalPrice,
        Instant occurredAt
) {
    public static OrderPlacedEvent of(UUID orderId, String customerEmail,
                                      String productName, Integer quantity,
                                      BigDecimal totalPrice) {
        return new OrderPlacedEvent(orderId, customerEmail, productName,
                quantity, totalPrice, Instant.now());
    }
}
