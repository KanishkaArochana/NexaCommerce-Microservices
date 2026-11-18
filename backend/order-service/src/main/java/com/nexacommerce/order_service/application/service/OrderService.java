package com.nexacommerce.order_service.application.service;

import com.nexacommerce.order_service.application.dto.*;
import com.nexacommerce.order_service.domain.model.Order;
import com.nexacommerce.order_service.domain.model.OrderItem;
import com.nexacommerce.order_service.domain.model.OrderStatus;
import com.nexacommerce.order_service.infrastructure.client.InventoryClient;
import com.nexacommerce.order_service.infrastructure.client.PaymentClient;
import com.nexacommerce.order_service.infrastructure.persistence.OrderRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;
    private final PaymentClient paymentClient;

    public OrderService(
            OrderRepository orderRepository,
            InventoryClient inventoryClient,
            PaymentClient paymentClient
    ) {
        this.orderRepository = orderRepository;
        this.inventoryClient = inventoryClient;
        this.paymentClient = paymentClient;
    }

    // Create a new order with inventory check and payment processing
    public OrderResponse create(OrderRequest request) {

        log.info("Starting order creation for userId: {}", request.userId());

        // 1. Check and reserve inventory for all items
        for (OrderItemRequest itemRequest : request.items()) {
            log.info("Checking inventory for productId: {}, quantity: {}", itemRequest.productId(), itemRequest.quantity());
            InventoryResponse inventory = inventoryClient.getInventoryByProductId(itemRequest.productId());

            if (inventory == null || inventory.availableQuantity() < itemRequest.quantity()) {
                throw new IllegalStateException("Insufficient stock for product ID: " + itemRequest.productId());
            }

            // Reserve stock
            inventoryClient.reserveStock(itemRequest.productId(), itemRequest.quantity());
        }

        // 2. Build and save initial order in PENDING status
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
        log.info("Order saved with ID: {} in PENDING status", savedOrder.getId());

        // 3. Process payment via payment-service
        try {
            PaymentRequest paymentRequest = new PaymentRequest(
                    savedOrder.getId(),
                    request.paymentMethod(),
                    savedOrder.getTotalAmount()
            );

            log.info("Processing payment for orderId: {}, amount: {}", savedOrder.getId(), savedOrder.getTotalAmount());
            PaymentResponse paymentResponse = paymentClient.processPayment(paymentRequest);

            if ("SUCCESS".equalsIgnoreCase(paymentResponse.status()) || "PENDING".equalsIgnoreCase(paymentResponse.status())) {
                savedOrder.updateStatus(OrderStatus.CONFIRMED);
                log.info("Payment successful. Order ID: {} is now CONFIRMED", savedOrder.getId());
            } else {
                savedOrder.updateStatus(OrderStatus.CANCELLED);
                log.warn("Payment failed. Order ID: {} is CANCELLED", savedOrder.getId());
            }
        } catch (Exception e) {
            savedOrder.updateStatus(OrderStatus.CANCELLED);
            log.error("Payment processing error for order ID: {}. Order marked as CANCELLED", savedOrder.getId(), e);
        }

        Order finalOrder = orderRepository.save(savedOrder);
        return toResponse(finalOrder);
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