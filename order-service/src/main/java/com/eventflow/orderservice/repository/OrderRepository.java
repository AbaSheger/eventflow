package com.eventflow.orderservice.repository;

import com.eventflow.orderservice.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {}
