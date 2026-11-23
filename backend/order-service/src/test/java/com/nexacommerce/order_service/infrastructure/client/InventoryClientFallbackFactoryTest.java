package com.nexacommerce.order_service.infrastructure.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("InventoryClientFallbackFactory Unit Tests")
class InventoryClientFallbackFactoryTest {

    private InventoryClientFallbackFactory fallbackFactory;
    private Throwable cause;

    @BeforeEach
    void setUp() {
        fallbackFactory = new InventoryClientFallbackFactory();
        cause = new RuntimeException("Inventory service connection timeout");
    }

    @Test
    @DisplayName("Should throw IllegalStateException when getInventoryByProductId fallback is triggered")
    void getInventoryByProductId_ShouldThrowIllegalStateException() {
        // Arrange
        InventoryClient fallbackClient = fallbackFactory.create(cause);

        // Act & Assert
        assertThatThrownBy(() -> fallbackClient.getInventoryByProductId(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Inventory service is currently unavailable. Please try again later.");
    }

    @Test
    @DisplayName("Should throw IllegalStateException when reserveStock fallback is triggered")
    void reserveStock_ShouldThrowIllegalStateException() {
        // Arrange
        InventoryClient fallbackClient = fallbackFactory.create(cause);

        // Act & Assert
        assertThatThrownBy(() -> fallbackClient.reserveStock(1L, 2))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Inventory service is currently unavailable. Could not reserve stock.");
    }
}
