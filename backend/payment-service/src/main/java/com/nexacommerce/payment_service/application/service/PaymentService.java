package com.nexacommerce.payment_service.application.service;

import com.nexacommerce.payment_service.application.dto.PaymentRequest;
import com.nexacommerce.payment_service.application.dto.PaymentResponse;
import com.nexacommerce.payment_service.domain.model.Payment;
import com.nexacommerce.payment_service.infrastructure.persistence.PaymentRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    // Create a new payment
    public PaymentResponse create(PaymentRequest request) {

        Payment payment = new Payment(
                request.orderId(),
                request.paymentMethod(),
                request.amount()
        );

        Payment savedPayment = paymentRepository.save(payment);

        return toResponse(savedPayment);
    }

    // Get payment by ID
    @Transactional(readOnly = true)
    public PaymentResponse getById(Long id) {

        Payment payment = findPayment(id);

        return toResponse(payment);
    }

    // Get payments by order ID
    @Transactional(readOnly = true)
    public List<PaymentResponse> getByOrderId(Long orderId) {

        if (orderId == null || orderId <= 0) {
            throw new IllegalArgumentException("Invalid order ID");
        }

        return paymentRepository.findByOrderId(orderId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // Get all payments
    @Transactional(readOnly = true)
    public List<PaymentResponse> getAll() {

        return paymentRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // Find payment by ID
    private Payment findPayment(Long id) {

        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid payment ID");
        }

        return paymentRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Payment not found with ID: " + id
                        )
                );
    }

    // Convert entity to response DTO
    private PaymentResponse toResponse(Payment payment) {

        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getPaymentMethod(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getTransactionId(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}
