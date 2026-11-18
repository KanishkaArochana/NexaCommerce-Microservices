package com.nexacommerce.order_service.application.dto;

import com.nexacommerce.order_service.domain.model.PaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        Long orderId,
        PaymentMethod paymentMethod,
        BigDecimal amount,
        String status,
        String transactionId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}