package com.nexacommerce.payment_service.presentation.controller;

import com.nexacommerce.payment_service.application.dto.PaymentRequest;
import com.nexacommerce.payment_service.application.dto.PaymentResponse;
import com.nexacommerce.payment_service.application.service.PaymentService;
import com.nexacommerce.payment_service.domain.model.PaymentMethod;
import com.nexacommerce.payment_service.domain.model.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@DisplayName("PaymentController WebMvc Tests")
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PaymentService paymentService;

    private PaymentResponse createSampleResponse() {
        return new PaymentResponse(
                1L,
                101L,
                PaymentMethod.CARD,
                new BigDecimal("150.00"),
                PaymentStatus.PENDING,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("POST /api/v1/payments Tests")
    class CreatePaymentEndpointTests {

        @Test
        @DisplayName("Should return 201 Created when given a valid PaymentRequest")
        void create_ValidRequest_ReturnsCreated() throws Exception {
            PaymentRequest request = new PaymentRequest(101L, PaymentMethod.CARD, new BigDecimal("150.00"));
            PaymentResponse response = createSampleResponse();

            when(paymentService.create(any(PaymentRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/v1/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.orderId").value(101L))
                    .andExpect(jsonPath("$.paymentMethod").value("CARD"))
                    .andExpect(jsonPath("$.amount").value(150.00))
                    .andExpect(jsonPath("$.status").value("PENDING"));

            verify(paymentService).create(any(PaymentRequest.class));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when orderId is missing")
        void create_MissingOrderId_ReturnsBadRequest() throws Exception {
            String json = """
                    {
                        "paymentMethod": "CARD",
                        "amount": 150.00
                    }
                    """;

            mockMvc.perform(post("/api/v1/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 Bad Request when orderId is negative")
        void create_NegativeOrderId_ReturnsBadRequest() throws Exception {
            String json = """
                    {
                        "orderId": -1,
                        "paymentMethod": "CARD",
                        "amount": 150.00
                    }
                    """;

            mockMvc.perform(post("/api/v1/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 Bad Request when paymentMethod is missing")
        void create_MissingPaymentMethod_ReturnsBadRequest() throws Exception {
            String json = """
                    {
                        "orderId": 101,
                        "amount": 150.00
                    }
                    """;

            mockMvc.perform(post("/api/v1/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 Bad Request when amount is zero or negative")
        void create_ZeroAmount_ReturnsBadRequest() throws Exception {
            String json = """
                    {
                        "orderId": 101,
                        "paymentMethod": "CARD",
                        "amount": 0.00
                    }
                    """;

            mockMvc.perform(post("/api/v1/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/payments/{id} Tests")
    class GetPaymentByIdEndpointTests {

        @Test
        @DisplayName("Should return 200 OK and PaymentResponse when payment exists")
        void getById_ExistingId_ReturnsOk() throws Exception {
            PaymentResponse response = createSampleResponse();

            when(paymentService.getById(1L)).thenReturn(response);

            mockMvc.perform(get("/api/v1/payments/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.orderId").value(101L))
                    .andExpect(jsonPath("$.paymentMethod").value("CARD"))
                    .andExpect(jsonPath("$.amount").value(150.00))
                    .andExpect(jsonPath("$.status").value("PENDING"));

            verify(paymentService).getById(1L);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/payments/order/{orderId} Tests")
    class GetPaymentsByOrderIdEndpointTests {

        @Test
        @DisplayName("Should return 200 OK and list of payments for given orderId")
        void getByOrderId_ValidOrderId_ReturnsOk() throws Exception {
            PaymentResponse response = createSampleResponse();

            when(paymentService.getByOrderId(101L)).thenReturn(List.of(response));

            mockMvc.perform(get("/api/v1/payments/order/101"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].id").value(1L))
                    .andExpect(jsonPath("$[0].orderId").value(101L));

            verify(paymentService).getByOrderId(101L);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/payments Tests")
    class GetAllPaymentsEndpointTests {

        @Test
        @DisplayName("Should return 200 OK and list of all payments")
        void getAll_ReturnsOk() throws Exception {
            PaymentResponse response = createSampleResponse();

            when(paymentService.getAll()).thenReturn(List.of(response));

            mockMvc.perform(get("/api/v1/payments"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].id").value(1L));

            verify(paymentService).getAll();
        }
    }
}
