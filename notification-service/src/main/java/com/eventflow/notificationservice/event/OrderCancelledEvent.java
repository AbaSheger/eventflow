package com.eventflow.notificationservice.event;

import java.time.Instant;
import java.util.UUID;

public record OrderCancelledEvent(
        UUID orderId,
        String customerEmail,
        String productName,
        Instant occurredAt
) {}
