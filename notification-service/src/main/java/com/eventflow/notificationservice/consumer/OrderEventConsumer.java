package com.eventflow.notificationservice.consumer;

import com.eventflow.notificationservice.event.OrderCancelledEvent;
import com.eventflow.notificationservice.event.OrderPlacedEvent;
import com.eventflow.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = "${kafka.topic.orders}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(@Payload Object rawEvent,
                        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                        @Header(KafkaHeaders.OFFSET) long offset) {

        log.debug("Received event from topic={} partition={} offset={}: {}",
                topic, partition, offset, rawEvent.getClass().getSimpleName());

        switch (rawEvent) {
            case OrderPlacedEvent event -> notificationService.handleOrderPlaced(event);
            case OrderCancelledEvent event -> notificationService.handleOrderCancelled(event);
            default -> log.warn("Unknown event type received: {}", rawEvent.getClass().getName());
        }
    }
}
