package com.eventflow.notificationservice.service;

import com.eventflow.notificationservice.event.OrderCancelledEvent;
import com.eventflow.notificationservice.event.OrderPlacedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${notification.from-email}")
    private String fromEmail;

    public void sendOrderConfirmation(OrderPlacedEvent event) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(event.customerEmail());
        message.setSubject("Order Confirmed — " + event.productName());
        message.setText("""
                Hi there,

                Your order has been placed successfully!

                Order ID:     %s
                Product:      %s
                Quantity:     %d
                Total Price:  $%.2f

                Thank you for shopping with EventFlow!
                """.formatted(
                event.orderId(), event.productName(),
                event.quantity(), event.totalPrice()
        ));
        mailSender.send(message);
        log.info("Confirmation email sent to {} for order {}", event.customerEmail(), event.orderId());
    }

    public void sendOrderCancellation(OrderCancelledEvent event) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(event.customerEmail());
        message.setSubject("Order Cancelled — " + event.productName());
        message.setText("""
                Hi there,

                Your order has been cancelled.

                Order ID: %s
                Product:  %s

                If this was a mistake, please place a new order on our website.

                — EventFlow Team
                """.formatted(event.orderId(), event.productName()));
        mailSender.send(message);
        log.info("Cancellation email sent to {} for order {}", event.customerEmail(), event.orderId());
    }
}
