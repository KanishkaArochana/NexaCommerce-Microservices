package com.nexacommerce.order_service.application.service;

import com.nexacommerce.order_service.application.dto.*;
import com.nexacommerce.order_service.domain.model.Order;
import com.nexacommerce.order_service.domain.model.OrderItem;
import com.nexacommerce.order_service.domain.model.OrderStatus;
import com.nexacommerce.order_service.domain.model.PaymentMethod;
import com.nexacommerce.order_service.infrastructure.client.InventoryClient;
import com.nexacommerce.order_service.infrastructure.client.PaymentClient;
import com.nexacommerce.order_service.infrastructure.persistence.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService Unit Tests")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private InventoryClient inventoryClient;

    @Mock
    private PaymentClient paymentClient;

    @InjectMocks
    private OrderService orderService;

    private Order testOrder;

    @BeforeEach
    void setUp() {
        testOrder = new Order(100L);
        OrderItem item = new OrderItem(1L, 2, new BigDecimal("50.00"));
        testOrder.addItem(item);
    }

    @Nested
    @DisplayName("Create Order Tests")
    class CreateOrderTests {

        // Order confirmed when stock is available and payment succeeds
        @Test
        @DisplayName("Should create order as CONFIRMED when stock is available and payment succeeds")
        void create_StockAvailableAndPaymentSuccess_ReturnsConfirmedOrder() {
            // Arrange
            OrderItemRequest itemRequest = new OrderItemRequest(1L, 2, new BigDecimal("50.00"));
            OrderRequest request = new OrderRequest(100L, PaymentMethod.CARD, List.of(itemRequest));

            InventoryResponse inventoryResponse = new InventoryResponse(1L, 1L, 10, 0, 10);
            PaymentResponse paymentResponse = new PaymentResponse(
                    1L, 1L, PaymentMethod.CARD, new BigDecimal("100.00"), "SUCCESS", "TXN-123",
                    LocalDateTime.now(), LocalDateTime.now()
            );

            when(inventoryClient.getInventoryByProductId(1L)).thenReturn(inventoryResponse);
            when(inventoryClient.reserveStock(1L, 2)).thenReturn(inventoryResponse);
            when(paymentClient.processPayment(any(PaymentRequest.class))).thenReturn(paymentResponse);
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            OrderResponse response = orderService.create(request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.userId()).isEqualTo(100L);
            assertThat(response.status()).isEqualTo(OrderStatus.CONFIRMED);
            assertThat(response.totalAmount()).isEqualByComparingTo(new BigDecimal("100.00"));

            verify(inventoryClient, times(1)).getInventoryByProductId(1L);
            verify(inventoryClient, times(1)).reserveStock(1L, 2);
            verify(paymentClient, times(1)).processPayment(any(PaymentRequest.class));
            verify(orderRepository, times(2)).save(any(Order.class));
        }

        // Throw error when stock is not enough
        @Test
        @DisplayName("Should throw IllegalStateException when inventory is insufficient")
        void create_InsufficientStock_ThrowsIllegalStateException() {
            // Arrange
            OrderItemRequest itemRequest = new OrderItemRequest(1L, 5, new BigDecimal("50.00"));
            OrderRequest request = new OrderRequest(100L, PaymentMethod.CARD, List.of(itemRequest));

            InventoryResponse lowStock = new InventoryResponse(1L, 1L, 2, 0, 2);
            when(inventoryClient.getInventoryByProductId(1L)).thenReturn(lowStock);

            // Act & Assert
            assertThatThrownBy(() -> orderService.create(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Insufficient stock for product ID: 1");

            verify(inventoryClient, times(1)).getInventoryByProductId(1L);
            verify(inventoryClient, never()).reserveStock(anyLong(), anyInt());
            verify(paymentClient, never()).processPayment(any(PaymentRequest.class));
            verify(orderRepository, never()).save(any(Order.class));
        }

        // Order cancelled when payment fails
        @Test
        @DisplayName("Should mark order as CANCELLED when payment fails")
        void create_PaymentFails_ReturnsCancelledOrder() {
            // Arrange
            OrderItemRequest itemRequest = new OrderItemRequest(1L, 2, new BigDecimal("50.00"));
            OrderRequest request = new OrderRequest(100L, PaymentMethod.CARD, List.of(itemRequest));

            InventoryResponse inventoryResponse = new InventoryResponse(1L, 1L, 10, 0, 10);
            PaymentResponse paymentFailedResponse = new PaymentResponse(
                    1L, 1L, PaymentMethod.CARD, new BigDecimal("100.00"), "FAILED", null,
                    LocalDateTime.now(), LocalDateTime.now()
            );

            when(inventoryClient.getInventoryByProductId(1L)).thenReturn(inventoryResponse);
            when(inventoryClient.reserveStock(1L, 2)).thenReturn(inventoryResponse);
            when(paymentClient.processPayment(any(PaymentRequest.class))).thenReturn(paymentFailedResponse);
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            OrderResponse response = orderService.create(request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
        }
    }

    @Nested
    @DisplayName("Get Order By ID Tests")
    class GetByIdTests {

        // Return order when ID exists
        @Test
        @DisplayName("Should return OrderResponse when order exists with given ID")
        void getById_ExistingId_ReturnsOrderResponse() {
            when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

            OrderResponse response = orderService.getById(1L);

            assertThat(response).isNotNull();
            assertThat(response.userId()).isEqualTo(100L);
            assertThat(response.items()).hasSize(1);
            verify(orderRepository, times(1)).findById(1L);
        }

        // Throw error when order not found by ID
        @Test
        @DisplayName("Should throw IllegalArgumentException when order does not exist with given ID")
        void getById_NonExistingId_ThrowsException() {
            when(orderRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.getById(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Order not found with ID: 999");
        }

        // Throw error when order ID is invalid
        @Test
        @DisplayName("Should throw IllegalArgumentException when ID is invalid")
        void getById_InvalidId_ThrowsException() {
            assertThatThrownBy(() -> orderService.getById(0L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid order ID");

            assertThatThrownBy(() -> orderService.getById(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid order ID");
        }
    }

    @Nested
    @DisplayName("Get All Orders Tests")
    class GetAllTests {

        // Return all orders when orders exist
        @Test
        @DisplayName("Should return list of OrderResponse when orders exist")
        void getAll_OrdersExist_ReturnsListOfOrderResponse() {
            when(orderRepository.findAll()).thenReturn(List.of(testOrder));

            List<OrderResponse> responseList = orderService.getAll();

            assertThat(responseList).isNotEmpty().hasSize(1);
            assertThat(responseList.get(0).userId()).isEqualTo(100L);
            verify(orderRepository, times(1)).findAll();
        }

        // Return empty list when no orders exist
        @Test
        @DisplayName("Should return empty list when no orders exist")
        void getAll_NoOrders_ReturnsEmptyList() {
            when(orderRepository.findAll()).thenReturn(List.of());

            List<OrderResponse> responseList = orderService.getAll();

            assertThat(responseList).isEmpty();
            verify(orderRepository, times(1)).findAll();
        }
    }

    @Nested
    @DisplayName("Get Orders By User ID Tests")
    class GetByUserIdTests {

        // Return orders for existing user ID
        @Test
        @DisplayName("Should return list of OrderResponse when orders exist for user")
        void getByUserId_ExistingUser_ReturnsListOfOrderResponse() {
            when(orderRepository.findByUserId(100L)).thenReturn(List.of(testOrder));

            List<OrderResponse> responseList = orderService.getByUserId(100L);

            assertThat(responseList).isNotEmpty().hasSize(1);
            assertThat(responseList.get(0).userId()).isEqualTo(100L);
            verify(orderRepository, times(1)).findByUserId(100L);
        }

        // Throw error when user ID is invalid
        @Test
        @DisplayName("Should throw IllegalArgumentException when userId is invalid")
        void getByUserId_InvalidUserId_ThrowsException() {
            assertThatThrownBy(() -> orderService.getByUserId(0L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid user ID");

            assertThatThrownBy(() -> orderService.getByUserId(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid user ID");
        }
    }

    @Nested
    @DisplayName("Update Order Status Tests")
    class UpdateStatusTests {

        // Update order status when order exists
        @Test
        @DisplayName("Should update status to CONFIRMED when order exists")
        void updateStatus_ExistingOrder_UpdatesStatusSuccessfully() {
            when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

            OrderResponse response = orderService.updateStatus(1L, OrderStatus.CONFIRMED);

            assertThat(response).isNotNull();
            assertThat(response.status()).isEqualTo(OrderStatus.CONFIRMED);
            verify(orderRepository, times(1)).findById(1L);
            verify(orderRepository, times(1)).save(testOrder);
        }

        // Throw error when order not found to update
        @Test
        @DisplayName("Should throw IllegalArgumentException when order does not exist")
        void updateStatus_NonExistingOrder_ThrowsException() {
            when(orderRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.updateStatus(999L, OrderStatus.CONFIRMED))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Order not found with ID: 999");
        }
    }
}