package com.nexacommerce.order_service.infrastructure.persistence;

import com.nexacommerce.order_service.domain.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserId(Long userId);
}