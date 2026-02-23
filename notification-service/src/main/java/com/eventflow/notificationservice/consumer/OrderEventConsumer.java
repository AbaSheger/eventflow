package com.eventflow.notificationservice.consumer;

import com.eventflow.notificationservice.event.OrderCancelledEvent;
import com.eventflow.notificationservice.event.OrderPlacedEvent;
import com.eventflow.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
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
    public void consume(ConsumerRecord<String, Object> record) {
        Object rawEvent = record.value();

        log.debug("Received event from topic={} partition={} offset={}: {}",
                record.topic(), record.partition(), record.offset(),
                rawEvent == null ? "null" : rawEvent.getClass().getSimpleName());

        if (rawEvent == null) {
            log.warn("Null event received at topic={} partition={} offset={}",
                    record.topic(), record.partition(), record.offset());
            return;
        }

        switch (rawEvent) {
            case OrderPlacedEvent event -> notificationService.handleOrderPlaced(event);
            case OrderCancelledEvent event -> notificationService.handleOrderCancelled(event);
            default -> log.warn("Unknown event type received: {}", rawEvent.getClass().getName());
        }
    }
}
