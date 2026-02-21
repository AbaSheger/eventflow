package com.eventflow.notificationservice.service;

import com.eventflow.notificationservice.event.OrderCancelledEvent;
import com.eventflow.notificationservice.event.OrderPlacedEvent;
import com.eventflow.notificationservice.model.Notification;
import com.eventflow.notificationservice.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void handleOrderPlaced_savesNotificationAsSent() {
        OrderPlacedEvent event = new OrderPlacedEvent(
                UUID.randomUUID(), "alice@example.com", "Laptop",
                1, new BigDecimal("999.99"), Instant.now()
        );

        doNothing().when(emailService).sendOrderConfirmation(event);
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        notificationService.handleOrderPlaced(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(Notification.DeliveryStatus.SENT);
        assertThat(saved.getType()).isEqualTo(Notification.NotificationType.ORDER_PLACED);
        assertThat(saved.getRecipientEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void handleOrderPlaced_emailFails_savesNotificationAsFailed() {
        OrderPlacedEvent event = new OrderPlacedEvent(
                UUID.randomUUID(), "fail@example.com", "Widget",
                2, new BigDecimal("29.99"), Instant.now()
        );

        doThrow(new RuntimeException("SMTP error")).when(emailService).sendOrderConfirmation(event);
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> notificationService.handleOrderPlaced(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("SMTP error");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(Notification.DeliveryStatus.FAILED);
        assertThat(saved.getErrorMessage()).isEqualTo("SMTP error");
    }

    @Test
    void handleOrderCancelled_savesNotificationAsSent() {
        OrderCancelledEvent event = new OrderCancelledEvent(
                UUID.randomUUID(), "bob@example.com", "Headphones", Instant.now()
        );

        doNothing().when(emailService).sendOrderCancellation(event);
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        notificationService.handleOrderCancelled(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        assertThat(captor.getValue().getType()).isEqualTo(Notification.NotificationType.ORDER_CANCELLED);
        assertThat(captor.getValue().getStatus()).isEqualTo(Notification.DeliveryStatus.SENT);
    }
}
