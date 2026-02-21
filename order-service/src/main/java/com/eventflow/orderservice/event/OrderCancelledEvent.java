package com.eventflow.orderservice.event;

import java.time.Instant;
import java.util.UUID;

public record OrderCancelledEvent(
        UUID orderId,
        String customerEmail,
        String productName,
        Instant occurredAt
) {
    public static OrderCancelledEvent of(UUID orderId, String customerEmail, String productName) {
        return new OrderCancelledEvent(orderId, customerEmail, productName, Instant.now());
    }
}
