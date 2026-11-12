package com.nexacommerce.order_service.application.service;

import com.nexacommerce.order_service.application.dto.*;
import com.nexacommerce.order_service.domain.model.Order;
import com.nexacommerce.order_service.domain.model.OrderItem;
import com.nexacommerce.order_service.domain.model.OrderStatus;
import com.nexacommerce.order_service.infrastructure.persistence.OrderRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    // Create a new order
    public OrderResponse create(OrderRequest request) {

        Order order = new Order(request.userId());

        for (OrderItemRequest itemRequest : request.items()) {

            OrderItem item = new OrderItem(
                    itemRequest.productId(),
                    itemRequest.quantity(),
                    itemRequest.unitPrice()
            );

            order.addItem(item);
        }

        Order savedOrder = orderRepository.save(order);

        return toResponse(savedOrder);
    }

    // Get order by ID
    @Transactional(readOnly = true)
    public OrderResponse getById(Long id) {

        Order order = findOrder(id);

        return toResponse(order);
    }

    // Get all orders
    @Transactional(readOnly = true)
    public List<OrderResponse> getAll() {

        return orderRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // Get orders belonging to a user
    @Transactional(readOnly = true)
    public List<OrderResponse> getByUserId(Long userId) {

        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("Invalid user ID");
        }

        return orderRepository.findByUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // Update order status
    public OrderResponse updateStatus(Long id, OrderStatus status) {

        Order order = findOrder(id);

        order.updateStatus(status);

        Order updatedOrder = orderRepository.save(order);

        return toResponse(updatedOrder);
    }

    // Find order by ID
    private Order findOrder(Long id) {

        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid order ID");
        }

        return orderRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Order not found with ID: " + id)
                );
    }

    // Convert entity to response DTO
    private OrderResponse toResponse(Order order) {

        List<OrderItemResponse> itemResponses = order.getItems()
                .stream()
                .map(item -> new OrderItemResponse(
                        item.getId(),
                        item.getProductId(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getSubtotal()
                ))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getOrderDate(),
                order.getStatus(),
                order.getTotalAmount(),
                itemResponses,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}