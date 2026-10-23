package com.nexacommerce.inventory_service.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Inventory Domain Model Tests")
class InventoryTest {

    @Nested
    @DisplayName("Constructor and Invariant Validations")
    class ConstructorTests {

        @Test
        @DisplayName("Should create inventory when given valid attributes")
        void shouldCreateInventorySuccessfully() {
            Inventory inventory = new Inventory(1L, 100, "Colombo Warehouse");

            assertNull(inventory.getId());
            assertEquals(1L, inventory.getProductId());
            assertEquals(100, inventory.getStockQuantity());
            assertEquals(0, inventory.getReservedQuantity());
            assertEquals(100, inventory.getAvailableQuantity());
            assertEquals("Colombo Warehouse", inventory.getWarehouseLocation());
            assertNotNull(inventory.getUpdatedAt());
        }

        @Test
        @DisplayName("Should throw exception when productId is null")
        void shouldThrowWhenProductIdIsNull() {
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> new Inventory(null, 100, "Warehouse A")
            );
            assertEquals("Invalid product ID", ex.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when productId is zero or negative")
        void shouldThrowWhenProductIdIsZeroOrNegative() {
            IllegalArgumentException ex1 = assertThrows(
                    IllegalArgumentException.class,
                    () -> new Inventory(0L, 100, "Warehouse A")
            );
            assertEquals("Invalid product ID", ex1.getMessage());

            IllegalArgumentException ex2 = assertThrows(
                    IllegalArgumentException.class,
                    () -> new Inventory(-5L, 100, "Warehouse A")
            );
            assertEquals("Invalid product ID", ex2.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when stockQuantity is negative")
        void shouldThrowWhenStockQuantityIsNegative() {
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> new Inventory(1L, -10, "Warehouse A")
            );
            assertEquals("Stock quantity cannot be negative", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("Available Quantity Calculations")
    class AvailableQuantityTests {

        @Test
        @DisplayName("Should calculate available quantity as stock minus reserved")
        void shouldCalculateAvailableQuantity() {
            Inventory inventory = new Inventory(1L, 100, "Warehouse A");
            inventory.reserveStock(30);

            assertEquals(100, inventory.getStockQuantity());
            assertEquals(30, inventory.getReservedQuantity());
            assertEquals(70, inventory.getAvailableQuantity());
        }
    }

    @Nested
    @DisplayName("Stock Update Tests")
    class UpdateStockTests {

        @Test
        @DisplayName("Should update stock quantity when new value is valid")
        void shouldUpdateStockSuccessfully() {
            Inventory inventory = new Inventory(1L, 50, "Warehouse A");
            inventory.updateStock(120);

            assertEquals(120, inventory.getStockQuantity());
            assertEquals(120, inventory.getAvailableQuantity());
        }

        @Test
        @DisplayName("Should throw exception when new stock is negative")
        void shouldThrowWhenUpdatingToNegativeStock() {
            Inventory inventory = new Inventory(1L, 50, "Warehouse A");

            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> inventory.updateStock(-5)
            );
            assertEquals("Stock quantity cannot be negative", ex.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when new stock is less than already reserved quantity")
        void shouldThrowWhenNewStockIsLessThanReserved() {
            Inventory inventory = new Inventory(1L, 50, "Warehouse A");
            inventory.reserveStock(20);

            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> inventory.updateStock(15)
            );
            assertEquals("Stock cannot be less than reserved quantity", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("Stock Reservation Tests")
    class ReserveStockTests {

        @Test
        @DisplayName("Should increase reserved quantity when reservation is valid")
        void shouldReserveStockSuccessfully() {
            Inventory inventory = new Inventory(1L, 100, "Warehouse A");
            inventory.reserveStock(40);

            assertEquals(40, inventory.getReservedQuantity());
            assertEquals(60, inventory.getAvailableQuantity());
        }

        @Test
        @DisplayName("Should allow reserving exact available quantity")
        void shouldAllowReservingExactAvailableQuantity() {
            Inventory inventory = new Inventory(1L, 50, "Warehouse A");
            inventory.reserveStock(50);

            assertEquals(50, inventory.getReservedQuantity());
            assertEquals(0, inventory.getAvailableQuantity());
        }

        @Test
        @DisplayName("Should throw exception when reservation quantity is zero or negative")
        void shouldThrowWhenReservationQuantityIsZeroOrNegative() {
            Inventory inventory = new Inventory(1L, 50, "Warehouse A");

            IllegalArgumentException ex1 = assertThrows(
                    IllegalArgumentException.class,
                    () -> inventory.reserveStock(0)
            );
            assertEquals("Reservation quantity must be positive", ex1.getMessage());

            IllegalArgumentException ex2 = assertThrows(
                    IllegalArgumentException.class,
                    () -> inventory.reserveStock(-10)
            );
            assertEquals("Reservation quantity must be positive", ex2.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when reservation exceeds available quantity")
        void shouldThrowWhenReservationExceedsAvailableQuantity() {
            Inventory inventory = new Inventory(1L, 50, "Warehouse A");

            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> inventory.reserveStock(51)
            );
            assertEquals("Insufficient available stock", ex.getMessage());
        }
    }
}
