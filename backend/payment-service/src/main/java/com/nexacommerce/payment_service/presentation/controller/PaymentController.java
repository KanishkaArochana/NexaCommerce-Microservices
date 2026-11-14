package com.nexacommerce.payment_service.presentation.controller;

import com.nexacommerce.payment_service.application.dto.PaymentRequest;
import com.nexacommerce.payment_service.application.dto.PaymentResponse;
import com.nexacommerce.payment_service.application.service.PaymentService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // Create payment
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse create(
            @Valid @RequestBody PaymentRequest request
    ) {
        return paymentService.create(request);
    }

    // Get payment by ID
    @GetMapping("/{id}")
    public PaymentResponse getById(@PathVariable Long id) {
        return paymentService.getById(id);
    }

    // Get payments by order ID
    @GetMapping("/order/{orderId}")
    public List<PaymentResponse> getByOrderId(
            @PathVariable Long orderId
    ) {
        return paymentService.getByOrderId(orderId);
    }

    // Get all payments
    @GetMapping
    public List<PaymentResponse> getAll() {
        return paymentService.getAll();
    }
}
