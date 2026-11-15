package com.nexacommerce.payment_service.application.dto;

import com.nexacommerce.payment_service.domain.model.PaymentMethod;
import com.nexacommerce.payment_service.domain.model.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(

        Long id,
        Long orderId,
        PaymentMethod paymentMethod,
        BigDecimal amount,
        PaymentStatus status,
        String transactionId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt

) {
}
