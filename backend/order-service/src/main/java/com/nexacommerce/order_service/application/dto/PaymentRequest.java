package com.nexacommerce.order_service.application.dto;

import com.nexacommerce.order_service.domain.model.PaymentMethod;
import java.math.BigDecimal;

public record PaymentRequest(
        Long orderId,
        PaymentMethod paymentMethod,
        BigDecimal amount
) {
}