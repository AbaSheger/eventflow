package com.eventflow.notificationservice.service;

import com.eventflow.notificationservice.event.OrderCancelledEvent;
import com.eventflow.notificationservice.event.OrderPlacedEvent;
import com.eventflow.notificationservice.model.Notification;
import com.eventflow.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;

    @Transactional
    public void handleOrderPlaced(OrderPlacedEvent event) {
        Notification notification = new Notification();
        notification.setOrderId(event.orderId());
        notification.setRecipientEmail(event.customerEmail());
        notification.setType(Notification.NotificationType.ORDER_PLACED);

        try {
            emailService.sendOrderConfirmation(event);
            notification.setStatus(Notification.DeliveryStatus.SENT);
        } catch (Exception ex) {
            log.error("Failed to send confirmation email for order {}: {}", event.orderId(), ex.getMessage());
            notification.setStatus(Notification.DeliveryStatus.FAILED);
            notification.setErrorMessage(ex.getMessage());
            throw ex; // re-throw so Kafka retry/DLT kicks in
        } finally {
            notificationRepository.save(notification);
        }
    }

    @Transactional
    public void handleOrderCancelled(OrderCancelledEvent event) {
        Notification notification = new Notification();
        notification.setOrderId(event.orderId());
        notification.setRecipientEmail(event.customerEmail());
        notification.setType(Notification.NotificationType.ORDER_CANCELLED);

        try {
            emailService.sendOrderCancellation(event);
            notification.setStatus(Notification.DeliveryStatus.SENT);
        } catch (Exception ex) {
            log.error("Failed to send cancellation email for order {}: {}", event.orderId(), ex.getMessage());
            notification.setStatus(Notification.DeliveryStatus.FAILED);
            notification.setErrorMessage(ex.getMessage());
            throw ex;
        } finally {
            notificationRepository.save(notification);
        }
    }

    @Transactional(readOnly = true)
    public List<Notification> getAllNotifications() {
        return notificationRepository.findAllByOrderByCreatedAtDesc();
    }
}
