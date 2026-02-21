package com.eventflow.orderservice.service;

import com.eventflow.orderservice.dto.CreateOrderRequest;
import com.eventflow.orderservice.dto.OrderResponse;
import com.eventflow.orderservice.event.OrderCancelledEvent;
import com.eventflow.orderservice.event.OrderPlacedEvent;
import com.eventflow.orderservice.exception.OrderNotFoundException;
import com.eventflow.orderservice.model.Order;
import com.eventflow.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topic.orders}")
    private String ordersTopic;

    @Transactional
    public OrderResponse placeOrder(CreateOrderRequest request) {
        Order order = new Order();
        order.setCustomerEmail(request.customerEmail());
        order.setProductName(request.productName());
        order.setQuantity(request.quantity());
        order.setTotalPrice(request.totalPrice());

        Order saved = orderRepository.save(order);
        log.info("Order {} persisted for customer {}", saved.getId(), saved.getCustomerEmail());

        OrderPlacedEvent event = OrderPlacedEvent.of(
                saved.getId(), saved.getCustomerEmail(),
                saved.getProductName(), saved.getQuantity(), saved.getTotalPrice()
        );

        kafkaTemplate.send(ordersTopic, saved.getId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish OrderPlacedEvent for order {}: {}", saved.getId(), ex.getMessage());
                    } else {
                        log.info("OrderPlacedEvent published for order {} to partition {}",
                                saved.getId(), result.getRecordMetadata().partition());
                    }
                });

        return OrderResponse.from(saved);
    }

    @Transactional
    public OrderResponse cancelOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (order.getStatus() == Order.OrderStatus.CANCELLED) {
            throw new IllegalStateException("Order " + orderId + " is already cancelled");
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);

        OrderCancelledEvent event = OrderCancelledEvent.of(
                saved.getId(), saved.getCustomerEmail(), saved.getProductName()
        );

        kafkaTemplate.send(ordersTopic, saved.getId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish OrderCancelledEvent for order {}: {}", saved.getId(), ex.getMessage());
                    } else {
                        log.info("OrderCancelledEvent published for order {}", saved.getId());
                    }
                });

        return OrderResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId) {
        return orderRepository.findById(orderId)
                .map(OrderResponse::from)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }
}
