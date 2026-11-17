package com.nexacommerce.order_service.infrastructure.client;

import com.nexacommerce.order_service.application.dto.PaymentRequest;
import com.nexacommerce.order_service.application.dto.PaymentResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "payment-service", url = "${PAYMENT_SERVICE_URL:http://localhost:8086}")
public interface PaymentClient {

    // Process a new payment for an order
    @PostMapping("/api/v1/payments")
    PaymentResponse processPayment(@RequestBody PaymentRequest request);

    // Get payment details by order ID
    @GetMapping("/api/v1/payments/order/{orderId}")
    PaymentResponse getPaymentByOrderId(@PathVariable("orderId") Long orderId);
}