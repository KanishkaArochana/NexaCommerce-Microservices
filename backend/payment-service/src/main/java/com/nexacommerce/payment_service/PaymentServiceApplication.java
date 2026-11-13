package com.nexacommerce.payment_service;

import com.nexacommerce.payment_service.domain.model.Payment;
import com.nexacommerce.payment_service.infrastructure.persistence.PaymentRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;
import java.util.UUID;

@SpringBootApplication
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }

    @Bean
    public CommandLineRunner testRunner(PaymentRepository paymentRepository) {
        return args -> {
            System.out.println(">>> TESTING PAYMENT SERVICE DATABASE OPERATIONS <<<");

            // 1. Create a sample payment
            Payment payment = new Payment(101L, new BigDecimal("150.00"), UUID.randomUUID().toString());
            Payment savedPayment = paymentRepository.save(payment);
            System.out.println(">>> Payment Saved! ID: " + savedPayment.getId() + ", Transaction: " + savedPayment.getTransactionId());

            // 2. Fetch payments by orderId
            System.out.println(">>> Fetching payments for Order ID 101:");
            paymentRepository.findByOrderId(101L).forEach(p ->
                    System.out.println("    - Payment ID: " + p.getId() + " | Amount: $" + p.getAmount() + " | Created: " + p.getCreatedAt())
            );
        };
    }
}
