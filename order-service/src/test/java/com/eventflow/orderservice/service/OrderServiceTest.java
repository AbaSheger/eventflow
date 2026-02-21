package com.eventflow.orderservice.service;

import com.eventflow.orderservice.dto.CreateOrderRequest;
import com.eventflow.orderservice.dto.OrderResponse;
import com.eventflow.orderservice.event.OrderCancelledEvent;
import com.eventflow.orderservice.event.OrderPlacedEvent;
import com.eventflow.orderservice.exception.OrderNotFoundException;
import com.eventflow.orderservice.model.Order;
import com.eventflow.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(orderService, "ordersTopic", "orders");
    }

    @Test
    void placeOrder_persistsOrderAndPublishesEvent() {
        // Arrange
        CreateOrderRequest request = new CreateOrderRequest(
                "alice@example.com", "Laptop", 1, new BigDecimal("999.99"));

        Order savedOrder = new Order();
        savedOrder.setId(UUID.randomUUID());
        savedOrder.setCustomerEmail(request.customerEmail());
        savedOrder.setProductName(request.productName());
        savedOrder.setQuantity(request.quantity());
        savedOrder.setTotalPrice(request.totalPrice());

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(CompletableFuture.completedFuture(null));

        // Act
        OrderResponse response = orderService.placeOrder(request);

        // Assert
        assertThat(response.customerEmail()).isEqualTo("alice@example.com");
        assertThat(response.status()).isEqualTo("PLACED");

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq("orders"), eq(savedOrder.getId().toString()), eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(OrderPlacedEvent.class);

        OrderPlacedEvent event = (OrderPlacedEvent) eventCaptor.getValue();
        assertThat(event.customerEmail()).isEqualTo("alice@example.com");
        assertThat(event.productName()).isEqualTo("Laptop");
    }

    @Test
    void cancelOrder_updatesStatusAndPublishesCancelEvent() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        Order existingOrder = new Order();
        existingOrder.setId(orderId);
        existingOrder.setCustomerEmail("bob@example.com");
        existingOrder.setProductName("Phone");
        existingOrder.setQuantity(2);
        existingOrder.setTotalPrice(new BigDecimal("599.00"));
        existingOrder.setStatus(Order.OrderStatus.PLACED);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(existingOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(existingOrder);
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(CompletableFuture.completedFuture(null));

        // Act
        OrderResponse response = orderService.cancelOrder(orderId);

        // Assert
        assertThat(response.status()).isEqualTo("CANCELLED");

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq("orders"), eq(orderId.toString()), eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(OrderCancelledEvent.class);
    }

    @Test
    void cancelOrder_alreadyCancelled_throwsIllegalStateException() {
        UUID orderId = UUID.randomUUID();
        Order order = new Order();
        order.setId(orderId);
        order.setStatus(Order.OrderStatus.CANCELLED);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(orderId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already cancelled");
    }

    @Test
    void getOrder_notFound_throwsOrderNotFoundException() {
        UUID missingId = UUID.randomUUID();
        when(orderRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrder(missingId))
                .isInstanceOf(OrderNotFoundException.class);
    }
}
