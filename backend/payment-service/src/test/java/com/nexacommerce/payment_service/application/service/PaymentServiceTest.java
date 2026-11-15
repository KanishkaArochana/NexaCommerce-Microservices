package com.nexacommerce.payment_service.application.service;

import com.nexacommerce.payment_service.application.dto.PaymentRequest;
import com.nexacommerce.payment_service.application.dto.PaymentResponse;
import com.nexacommerce.payment_service.domain.model.Payment;
import com.nexacommerce.payment_service.domain.model.PaymentMethod;
import com.nexacommerce.payment_service.domain.model.PaymentStatus;
import com.nexacommerce.payment_service.infrastructure.persistence.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService Unit Tests")
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentService paymentService;

    private Payment testPayment;

    @BeforeEach
    void setUp() {
        testPayment = new Payment(101L, PaymentMethod.CARD, new BigDecimal("150.00"));
    }

    @Nested
    @DisplayName("Create Payment Tests")
    class CreatePaymentTests {

        @Test
        @DisplayName("Should create and save a new payment when given a valid PaymentRequest")
        void create_ValidRequest_ReturnsPaymentResponse() {
            PaymentRequest request = new PaymentRequest(101L, PaymentMethod.CARD, new BigDecimal("150.00"));

            when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

            PaymentResponse response = paymentService.create(request);

            assertThat(response).isNotNull();
            assertThat(response.orderId()).isEqualTo(101L);
            assertThat(response.paymentMethod()).isEqualTo(PaymentMethod.CARD);
            assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("150.00"));
            assertThat(response.status()).isEqualTo(PaymentStatus.PENDING);

            verify(paymentRepository, times(1)).save(any(Payment.class));
        }
    }

    @Nested
    @DisplayName("Get Payment By ID Tests")
    class GetPaymentByIdTests {

        @Test
        @DisplayName("Should return PaymentResponse when payment exists for given ID")
        void getById_ExistingId_ReturnsPaymentResponse() {
            when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));

            PaymentResponse response = paymentService.getById(1L);

            assertThat(response).isNotNull();
            assertThat(response.orderId()).isEqualTo(101L);
            assertThat(response.paymentMethod()).isEqualTo(PaymentMethod.CARD);
            assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("150.00"));
            assertThat(response.status()).isEqualTo(PaymentStatus.PENDING);

            verify(paymentRepository, times(1)).findById(1L);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when payment does not exist for given ID")
        void getById_NonExistentId_ThrowsException() {
            when(paymentRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.getById(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Payment not found with ID: 999");

            verify(paymentRepository, times(1)).findById(999L);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when ID is null")
        void getById_NullId_ThrowsException() {
            assertThatThrownBy(() -> paymentService.getById(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Invalid payment ID");

            verify(paymentRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when ID is less than or equal to zero")
        void getById_ZeroOrNegativeId_ThrowsException() {
            assertThatThrownBy(() -> paymentService.getById(0L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Invalid payment ID");

            assertThatThrownBy(() -> paymentService.getById(-1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Invalid payment ID");

            verify(paymentRepository, never()).findById(any());
        }
    }

    @Nested
    @DisplayName("Get Payments By Order ID Tests")
    class GetPaymentsByOrderIdTests {

        @Test
        @DisplayName("Should return list of PaymentResponse for given order ID")
        void getByOrderId_ValidOrderId_ReturnsPaymentList() {
            when(paymentRepository.findByOrderId(101L)).thenReturn(List.of(testPayment));

            List<PaymentResponse> responses = paymentService.getByOrderId(101L);

            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).orderId()).isEqualTo(101L);
            assertThat(responses.get(0).paymentMethod()).isEqualTo(PaymentMethod.CARD);

            verify(paymentRepository, times(1)).findByOrderId(101L);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when order ID is null")
        void getByOrderId_NullOrderId_ThrowsException() {
            assertThatThrownBy(() -> paymentService.getByOrderId(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Invalid order ID");

            verify(paymentRepository, never()).findByOrderId(any());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when order ID is less than or equal to zero")
        void getByOrderId_ZeroOrNegativeOrderId_ThrowsException() {
            assertThatThrownBy(() -> paymentService.getByOrderId(0L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Invalid order ID");

            assertThatThrownBy(() -> paymentService.getByOrderId(-5L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Invalid order ID");

            verify(paymentRepository, never()).findByOrderId(any());
        }
    }

    @Nested
    @DisplayName("Get All Payments Tests")
    class GetAllPaymentsTests {

        @Test
        @DisplayName("Should return all payments")
        void getAll_ReturnsPaymentList() {
            Payment secondPayment = new Payment(102L, PaymentMethod.WALLET, new BigDecimal("75.50"));
            when(paymentRepository.findAll()).thenReturn(List.of(testPayment, secondPayment));

            List<PaymentResponse> responses = paymentService.getAll();

            assertThat(responses).hasSize(2);
            assertThat(responses.get(0).orderId()).isEqualTo(101L);
            assertThat(responses.get(1).orderId()).isEqualTo(102L);

            verify(paymentRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("Should return empty list when no payments exist")
        void getAll_NoPayments_ReturnsEmptyList() {
            when(paymentRepository.findAll()).thenReturn(List.of());

            List<PaymentResponse> responses = paymentService.getAll();

            assertThat(responses).isEmpty();

            verify(paymentRepository, times(1)).findAll();
        }
    }
}
