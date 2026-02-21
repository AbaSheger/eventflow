package com.eventflow.notificationservice.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

// Mirror of the order-service event — shared via JSON deserialization (no shared JAR)
public record OrderPlacedEvent(
        UUID orderId,
        String customerEmail,
        String productName,
        Integer quantity,
        BigDecimal totalPrice,
        Instant occurredAt
) {}
