package com.nexacommerce.order_service.application.service;

import com.nexacommerce.order_service.application.dto.OrderItemRequest;
import com.nexacommerce.order_service.application.dto.OrderRequest;
import com.nexacommerce.order_service.application.dto.OrderResponse;
import com.nexacommerce.order_service.domain.model.Order;
import com.nexacommerce.order_service.domain.model.OrderItem;
import com.nexacommerce.order_service.domain.model.OrderStatus;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService Unit Tests")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

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

        @Test
        @DisplayName("Should create and save a new order when given a valid OrderRequest")
        void create_ValidRequest_ReturnsOrderResponse() {
            // Arrange
            OrderItemRequest itemRequest = new OrderItemRequest(1L, 2, new BigDecimal("50.00"));
            OrderRequest request = new OrderRequest(100L, List.of(itemRequest));

            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            OrderResponse response = orderService.create(request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.userId()).isEqualTo(100L);
            assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
            assertThat(response.totalAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
            assertThat(response.items()).hasSize(1);
            assertThat(response.items().get(0).productId()).isEqualTo(1L);
            assertThat(response.items().get(0).quantity()).isEqualTo(2);
            assertThat(response.items().get(0).subtotal()).isEqualByComparingTo(new BigDecimal("100.00"));

            verify(orderRepository, times(1)).save(any(Order.class));
        }
    }

    @Nested
    @DisplayName("Get Order By ID Tests")
    class GetByIdTests {

        @Test
        @DisplayName("Should return OrderResponse when order exists with given ID")
        void getById_ExistingId_ReturnsOrderResponse() {
            // Arrange
            when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

            // Act
            OrderResponse response = orderService.getById(1L);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.userId()).isEqualTo(100L);
            assertThat(response.items()).hasSize(1);
            verify(orderRepository, times(1)).findById(1L);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when order does not exist with given ID")
        void getById_NonExistingId_ThrowsException() {
            // Arrange
            when(orderRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> orderService.getById(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Order not found with ID: 999");

            verify(orderRepository, times(1)).findById(999L);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when order ID is null, zero, or negative")
        void getById_InvalidId_ThrowsException() {
            assertThatThrownBy(() -> orderService.getById(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Invalid order ID");

            assertThatThrownBy(() -> orderService.getById(0L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Invalid order ID");

            assertThatThrownBy(() -> orderService.getById(-5L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Invalid order ID");

            verifyNoInteractions(orderRepository);
        }
    }

    @Nested
    @DisplayName("Get All Orders Tests")
    class GetAllTests {

        @Test
        @DisplayName("Should return a list of all order responses")
        void getAll_ReturnsListOfOrderResponses() {
            // Arrange
            when(orderRepository.findAll()).thenReturn(List.of(testOrder));

            // Act
            List<OrderResponse> responses = orderService.getAll();

            // Assert
            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).userId()).isEqualTo(100L);
            verify(orderRepository, times(1)).findAll();
        }
    }

    @Nested
    @DisplayName("Get Orders By User ID Tests")
    class GetByUserIdTests {

        @Test
        @DisplayName("Should return user orders when valid user ID is provided")
        void getByUserId_ValidUserId_ReturnsUserOrders() {
            // Arrange
            when(orderRepository.findByUserId(100L)).thenReturn(List.of(testOrder));

            // Act
            List<OrderResponse> responses = orderService.getByUserId(100L);

            // Assert
            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).userId()).isEqualTo(100L);
            verify(orderRepository, times(1)).findByUserId(100L);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when user ID is null, zero, or negative")
        void getByUserId_InvalidUserId_ThrowsException() {
            assertThatThrownBy(() -> orderService.getByUserId(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Invalid user ID");

            assertThatThrownBy(() -> orderService.getByUserId(-1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Invalid user ID");

            verifyNoInteractions(orderRepository);
        }
    }

    @Nested
    @DisplayName("Update Order Status Tests")
    class UpdateStatusTests {

        @Test
        @DisplayName("Should update status and save when order exists")
        void updateStatus_ExistingOrder_UpdatesStatusAndSaves() {
            // Arrange
            when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            OrderResponse response = orderService.updateStatus(1L, OrderStatus.CONFIRMED);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.status()).isEqualTo(OrderStatus.CONFIRMED);
            verify(orderRepository, times(1)).findById(1L);
            verify(orderRepository, times(1)).save(testOrder);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when updating status for non-existing order")
        void updateStatus_NonExistingOrder_ThrowsException() {
            // Arrange
            when(orderRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> orderService.updateStatus(999L, OrderStatus.CANCELLED))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Order not found with ID: 999");

            verify(orderRepository, times(1)).findById(999L);
            verify(orderRepository, never()).save(any());
        }
    }
}
