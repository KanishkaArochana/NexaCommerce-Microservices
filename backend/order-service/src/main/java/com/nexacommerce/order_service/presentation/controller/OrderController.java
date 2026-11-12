package com.nexacommerce.order_service.presentation.controller;

import com.nexacommerce.order_service.application.dto.OrderRequest;
import com.nexacommerce.order_service.application.dto.OrderResponse;
import com.nexacommerce.order_service.application.service.OrderService;
import com.nexacommerce.order_service.domain.model.OrderStatus;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // Create order
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@Valid @RequestBody OrderRequest request) {
        return orderService.create(request);
    }

    // Get order by ID
    @GetMapping("/{id}")
    public OrderResponse getById(@PathVariable Long id) {
        return orderService.getById(id);
    }

    // Get all orders
    @GetMapping
    public List<OrderResponse> getAll() {
        return orderService.getAll();
    }

    // Get orders by user ID
    @GetMapping("/user/{userId}")
    public List<OrderResponse> getByUserId(@PathVariable Long userId) {
        return orderService.getByUserId(userId);
    }

    // Update order status
    @PatchMapping("/{id}/status")
    public OrderResponse updateStatus(
            @PathVariable Long id,
            @RequestParam OrderStatus status
    ) {
        return orderService.updateStatus(id, status);
    }
}