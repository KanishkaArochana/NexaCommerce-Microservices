package com.nexacommerce.order_service.presentation.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import com.nexacommerce.order_service.application.dto.OrderItemRequest;
import com.nexacommerce.order_service.application.dto.OrderItemResponse;
import com.nexacommerce.order_service.application.dto.OrderRequest;
import com.nexacommerce.order_service.application.dto.OrderResponse;
import com.nexacommerce.order_service.application.service.OrderService;
import com.nexacommerce.order_service.domain.model.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@DisplayName("OrderController WebMvc Tests")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    private OrderResponse createSampleResponse() {
        OrderItemResponse itemResponse = new OrderItemResponse(
                1L,
                10L,
                2,
                new BigDecimal("50.00"),
                new BigDecimal("100.00")
        );

        return new OrderResponse(
                1L,
                100L,
                LocalDateTime.now(),
                OrderStatus.PENDING,
                new BigDecimal("100.00"),
                List.of(itemResponse),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("POST /api/v1/orders - Create Order")
    class CreateOrderEndpointTests {

        @Test
        @DisplayName("Should return HTTP 201 Created and OrderResponse when given a valid OrderRequest")
        void create_ValidRequest_Returns201Created() throws Exception {
            OrderItemRequest itemRequest = new OrderItemRequest(10L, 2, new BigDecimal("50.00"));
            OrderRequest request = new OrderRequest(100L, List.of(itemRequest));
            OrderResponse response = createSampleResponse();

            when(orderService.create(any(OrderRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/v1/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.userId").value(100L))
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.totalAmount").value(100.00))
                    .andExpect(jsonPath("$.items[0].productId").value(10L));

            verify(orderService).create(any(OrderRequest.class));
        }

        @Test
        @DisplayName("Should return HTTP 400 Bad Request when request body violates validation constraints")
        void create_InvalidRequest_Returns400BadRequest() throws Exception {
            OrderRequest invalidRequest = new OrderRequest(null, List.of());

            mockMvc.perform(post("/api/v1/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/orders/{id} - Get Order By ID")
    class GetOrderByIdEndpointTests {

        @Test
        @DisplayName("Should return HTTP 200 OK and OrderResponse when order exists with given ID")
        void getById_ExistingId_Returns200Ok() throws Exception {
            OrderResponse response = createSampleResponse();
            when(orderService.getById(1L)).thenReturn(response);

            mockMvc.perform(get("/api/v1/orders/{id}", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.userId").value(100L))
                    .andExpect(jsonPath("$.status").value("PENDING"));

            verify(orderService).getById(1L);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/orders - Get All Orders")
    class GetAllOrdersEndpointTests {

        @Test
        @DisplayName("Should return HTTP 200 OK with list of all orders")
        void getAll_Returns200OkWithList() throws Exception {
            OrderResponse response = createSampleResponse();
            when(orderService.getAll()).thenReturn(List.of(response));

            mockMvc.perform(get("/api/v1/orders"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").value(1L));

            verify(orderService).getAll();
        }
    }

    @Nested
    @DisplayName("GET /api/v1/orders/user/{userId} - Get Orders By User ID")
    class GetOrdersByUserIdEndpointTests {

        @Test
        @DisplayName("Should return HTTP 200 OK with orders belonging to given user ID")
        void getByUserId_Returns200OkWithUserOrders() throws Exception {
            OrderResponse response = createSampleResponse();
            when(orderService.getByUserId(100L)).thenReturn(List.of(response));

            mockMvc.perform(get("/api/v1/orders/user/{userId}", 100L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].userId").value(100L));

            verify(orderService).getByUserId(100L);
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/orders/{id}/status - Update Order Status")
    class UpdateStatusEndpointTests {

        @Test
        @DisplayName("Should return HTTP 200 OK and updated OrderResponse when status is updated")
        void updateStatus_Returns200Ok() throws Exception {
            OrderItemResponse itemResponse = new OrderItemResponse(
                    1L,
                    10L,
                    2,
                    new BigDecimal("50.00"),
                    new BigDecimal("100.00")
            );

            OrderResponse updatedResponse = new OrderResponse(
                    1L,
                    100L,
                    LocalDateTime.now(),
                    OrderStatus.CONFIRMED,
                    new BigDecimal("100.00"),
                    List.of(itemResponse),
                    LocalDateTime.now(),
                    LocalDateTime.now()
            );

            when(orderService.updateStatus(eq(1L), eq(OrderStatus.CONFIRMED))).thenReturn(updatedResponse);

            mockMvc.perform(patch("/api/v1/orders/{id}/status", 1L)
                            .param("status", "CONFIRMED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.status").value("CONFIRMED"));

            verify(orderService).updateStatus(1L, OrderStatus.CONFIRMED);
        }
    }
}
