package com.nexacommerce.order_service.infrastructure.client;

import com.nexacommerce.order_service.application.dto.PaymentRequest;
import com.nexacommerce.order_service.application.dto.PaymentResponse;
import com.nexacommerce.order_service.domain.model.PaymentMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PaymentClientFallbackFactory Unit Tests")
class PaymentClientFallbackFactoryTest {

    private PaymentClientFallbackFactory fallbackFactory;
    private Throwable cause;

    @BeforeEach
    void setUp() {
        fallbackFactory = new PaymentClientFallbackFactory();
        cause = new RuntimeException("Payment service connection refused");
    }

    @Test
    @DisplayName("Should return FAILED PaymentResponse when processPayment fallback is triggered")
    void processPayment_ShouldReturnFailedPaymentResponse() {
        // Arrange
        PaymentClient fallbackClient = fallbackFactory.create(cause);
        PaymentRequest request = new PaymentRequest(10L, PaymentMethod.CARD, new BigDecimal("500.00"));

        // Act
        PaymentResponse response = fallbackClient.processPayment(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.orderId()).isEqualTo(10L);
        assertThat(response.paymentMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(response.status()).isEqualTo("FAILED");
        assertThat(response.id()).isNull();
        assertThat(response.transactionId()).isNull();
    }

    @Test
    @DisplayName("Should return null when getPaymentByOrderId fallback is triggered")
    void getPaymentByOrderId_ShouldReturnNull() {
        // Arrange
        PaymentClient fallbackClient = fallbackFactory.create(cause);

        // Act
        PaymentResponse response = fallbackClient.getPaymentByOrderId(10L);

        // Assert
        assertThat(response).isNull();
    }
}
