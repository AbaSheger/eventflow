package com.eventflow.orderservice.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateOrderRequest(

        @NotBlank(message = "Customer email is required")
        @Email(message = "Must be a valid email")
        String customerEmail,

        @NotBlank(message = "Product name is required")
        String productName,

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity,

        @NotNull(message = "Total price is required")
        @DecimalMin(value = "0.01", message = "Total price must be positive")
        BigDecimal totalPrice
) {}
