package com.nexacommerce.order_service.infrastructure.client;

import com.nexacommerce.order_service.application.dto.PaymentRequest;
import com.nexacommerce.order_service.application.dto.PaymentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class PaymentClientFallbackFactory implements FallbackFactory<PaymentClient> {

    private static final Logger log = LoggerFactory.getLogger(PaymentClientFallbackFactory.class);

    @Override
    public PaymentClient create(Throwable cause) {
        return new PaymentClient() {

            // Fallback when payment-service is down during payment processing
            @Override
            public PaymentResponse processPayment(PaymentRequest request) {
                log.error("Fallback: Payment service is unavailable for processPayment(orderId: {}). Reason: {}",
                        request.orderId(), cause.getMessage());

                // Return FAILED status so OrderService safely marks order as CANCELLED
                return new PaymentResponse(
                        null,
                        request.orderId(),
                        request.paymentMethod(),
                        request.amount(),
                        "FAILED",
                        null,
                        LocalDateTime.now(),
                        LocalDateTime.now()
                );
            }

            // Fallback when payment-service is down during fetching payment by orderId
            @Override
            public PaymentResponse getPaymentByOrderId(Long orderId) {
                log.error("Fallback: Payment service is unavailable for getPaymentByOrderId({}). Reason: {}",
                        orderId, cause.getMessage());
                return null;
            }
        };
    }
}