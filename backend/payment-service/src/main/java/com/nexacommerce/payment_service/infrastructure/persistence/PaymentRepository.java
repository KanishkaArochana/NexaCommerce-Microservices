package com.nexacommerce.payment_service.infrastructure.persistence;

import com.nexacommerce.payment_service.domain.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByOrderId(Long orderId);
}
