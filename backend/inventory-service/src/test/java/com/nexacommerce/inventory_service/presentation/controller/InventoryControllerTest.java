package com.nexacommerce.inventory_service.presentation.controller;

import tools.jackson.databind.ObjectMapper;
import com.nexacommerce.inventory_service.application.dto.InventoryRequest;
import com.nexacommerce.inventory_service.application.dto.InventoryResponse;
import com.nexacommerce.inventory_service.application.service.InventoryService;
import com.nexacommerce.inventory_service.presentation.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryController REST API Endpoints Tests")
class InventoryControllerTest {

    private static final String BASE_URL = "/api/v1/inventory";

    private MockMvc mockMvc;

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private InventoryController inventoryController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(inventoryController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/inventory - Should create and return 200 OK")
    void shouldCreateInventory() throws Exception {
        InventoryRequest request = new InventoryRequest(1L, 100, "Warehouse A");
        InventoryResponse response = new InventoryResponse(
                10L, 1L, 100, 0, 100, "Warehouse A", LocalDateTime.now()
        );

        when(inventoryService.create(any(InventoryRequest.class))).thenReturn(response);

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.stockQuantity").value(100))
                .andExpect(jsonPath("$.availableQuantity").value(100));
    }

    @Test
    @DisplayName("GET /api/v1/inventory/{productId} - Should return 200 OK when found")
    void shouldGetInventoryByProductId() throws Exception {
        InventoryResponse response = new InventoryResponse(
                10L, 1L, 80, 10, 70, "Warehouse A", LocalDateTime.now()
        );

        when(inventoryService.getByProductId(1L)).thenReturn(response);

        mockMvc.perform(get(BASE_URL + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.stockQuantity").value(80))
                .andExpect(jsonPath("$.reservedQuantity").value(10))
                .andExpect(jsonPath("$.availableQuantity").value(70));
    }

    @Test
    @DisplayName("GET /api/v1/inventory/{productId} - Should return 404 NOT FOUND via GlobalExceptionHandler")
    void shouldReturn404WhenProductNotFound() throws Exception {
        when(inventoryService.getByProductId(999L))
                .thenThrow(new IllegalArgumentException("Inventory not found for product: 999"));

        mockMvc.perform(get(BASE_URL + "/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Inventory not found for product: 999"));
    }

    @Test
    @DisplayName("GET /api/v1/inventory - Should return all inventories with 200 OK")
    void shouldGetAllInventories() throws Exception {
        InventoryResponse resp1 = new InventoryResponse(1L, 100L, 50, 0, 50, "WH1", LocalDateTime.now());
        InventoryResponse resp2 = new InventoryResponse(2L, 101L, 30, 0, 30, "WH2", LocalDateTime.now());

        when(inventoryService.getAll()).thenReturn(List.of(resp1, resp2));

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].productId").value(100))
                .andExpect(jsonPath("$[1].productId").value(101));
    }

    @Test
    @DisplayName("PUT /api/v1/inventory/{productId}?quantity=... - Should update stock and return 200 OK")
    void shouldUpdateStock() throws Exception {
        InventoryResponse response = new InventoryResponse(
                1L, 5L, 200, 0, 200, "WH1", LocalDateTime.now()
        );

        when(inventoryService.updateStock(eq(5L), eq(200))).thenReturn(response);

        mockMvc.perform(put(BASE_URL + "/5").param("quantity", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(5))
                .andExpect(jsonPath("$.stockQuantity").value(200));
    }

    @Test
    @DisplayName("POST /api/v1/inventory/{productId}/reserve?quantity=... - Should reserve stock and return 200 OK")
    void shouldReserveStock() throws Exception {
        InventoryResponse response = new InventoryResponse(
                1L, 5L, 200, 15, 185, "WH1", LocalDateTime.now()
        );

        when(inventoryService.reserveStock(eq(5L), eq(15))).thenReturn(response);

        mockMvc.perform(post(BASE_URL + "/5/reserve").param("quantity", "15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservedQuantity").value(15))
                .andExpect(jsonPath("$.availableQuantity").value(185));
    }

    @Test
    @DisplayName("POST /api/v1/inventory/{productId}/reserve - Should return 400 BAD REQUEST when stock insufficient")
    void shouldReturn400WhenInsufficientStock() throws Exception {
        when(inventoryService.reserveStock(eq(5L), eq(500)))
                .thenThrow(new IllegalArgumentException("Insufficient available stock"));

        mockMvc.perform(post(BASE_URL + "/5/reserve").param("quantity", "500"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Insufficient available stock"));
    }
}
