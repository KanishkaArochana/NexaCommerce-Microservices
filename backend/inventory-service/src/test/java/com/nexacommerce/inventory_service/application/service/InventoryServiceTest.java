package com.nexacommerce.inventory_service.application.service;

import com.nexacommerce.inventory_service.application.dto.InventoryRequest;
import com.nexacommerce.inventory_service.application.dto.InventoryResponse;
import com.nexacommerce.inventory_service.domain.model.Inventory;
import com.nexacommerce.inventory_service.infrastructure.persistence.InventoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryService Business Logic Tests")
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private InventoryService inventoryService;

    @Nested
    @DisplayName("Create Inventory Tests")
    class CreateTests {

        @Test
        @DisplayName("Should create and return inventory response when product does not exist")
        void shouldCreateInventorySuccessfully() {
            InventoryRequest request = new InventoryRequest(1L, 100, "Warehouse Colombo");
            Inventory savedInventory = new Inventory(1L, 100, "Warehouse Colombo");

            when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.empty());
            when(inventoryRepository.save(any(Inventory.class))).thenReturn(savedInventory);

            InventoryResponse response = inventoryService.create(request);

            assertNotNull(response);
            assertEquals(1L, response.productId());
            assertEquals(100, response.stockQuantity());
            assertEquals(0, response.reservedQuantity());
            assertEquals(100, response.availableQuantity());
            assertEquals("Warehouse Colombo", response.warehouseLocation());

            verify(inventoryRepository, times(1)).findByProductId(1L);
            verify(inventoryRepository, times(1)).save(any(Inventory.class));
        }

        @Test
        @DisplayName("Should throw exception when inventory already exists for product")
        void shouldThrowWhenProductAlreadyExists() {
            InventoryRequest request = new InventoryRequest(1L, 100, "Warehouse Colombo");
            Inventory existing = new Inventory(1L, 50, "Warehouse Colombo");

            when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(existing));

            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> inventoryService.create(request)
            );

            assertEquals("Inventory already exists for this product", ex.getMessage());
            verify(inventoryRepository, times(1)).findByProductId(1L);
            verify(inventoryRepository, never()).save(any(Inventory.class));
        }
    }

    @Nested
    @DisplayName("Get Inventory Tests")
    class GetTests {

        @Test
        @DisplayName("Should return inventory response when product exists")
        void shouldReturnInventoryWhenFound() {
            Inventory inventory = new Inventory(1L, 80, "Warehouse Colombo");
            when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));

            InventoryResponse response = inventoryService.getByProductId(1L);

            assertNotNull(response);
            assertEquals(1L, response.productId());
            assertEquals(80, response.stockQuantity());
            verify(inventoryRepository, times(1)).findByProductId(1L);
        }

        @Test
        @DisplayName("Should throw exception when product does not exist")
        void shouldThrowWhenNotFound() {
            when(inventoryRepository.findByProductId(999L)).thenReturn(Optional.empty());

            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> inventoryService.getByProductId(999L)
            );

            assertEquals("Inventory not found for product: 999", ex.getMessage());
            verify(inventoryRepository, times(1)).findByProductId(999L);
        }

        @Test
        @DisplayName("Should return all inventory items")
        void shouldReturnAllInventories() {
            Inventory inv1 = new Inventory(1L, 50, "Warehouse A");
            Inventory inv2 = new Inventory(2L, 100, "Warehouse B");

            when(inventoryRepository.findAll()).thenReturn(List.of(inv1, inv2));

            List<InventoryResponse> results = inventoryService.getAll();

            assertEquals(2, results.size());
            assertEquals(1L, results.get(0).productId());
            assertEquals(2L, results.get(1).productId());
            verify(inventoryRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("Should return empty list when no inventories exist")
        void shouldReturnEmptyListWhenNoneExist() {
            when(inventoryRepository.findAll()).thenReturn(Collections.emptyList());

            List<InventoryResponse> results = inventoryService.getAll();

            assertTrue(results.isEmpty());
            verify(inventoryRepository, times(1)).findAll();
        }
    }

    @Nested
    @DisplayName("Update Stock Tests")
    class UpdateStockTests {

        @Test
        @DisplayName("Should update stock quantity and return updated response")
        void shouldUpdateStockSuccessfully() {
            Inventory inventory = new Inventory(1L, 50, "Warehouse A");
            when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));
            when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));

            InventoryResponse response = inventoryService.updateStock(1L, 120);

            assertNotNull(response);
            assertEquals(120, response.stockQuantity());
            assertEquals(120, response.availableQuantity());
            verify(inventoryRepository, times(1)).save(inventory);
        }

        @Test
        @DisplayName("Should throw exception when updating stock for non-existing product")
        void shouldThrowWhenUpdatingNonExistingProduct() {
            when(inventoryRepository.findByProductId(999L)).thenReturn(Optional.empty());

            assertThrows(
                    IllegalArgumentException.class,
                    () -> inventoryService.updateStock(999L, 100)
            );

            verify(inventoryRepository, never()).save(any(Inventory.class));
        }
    }

    @Nested
    @DisplayName("Reserve Stock Tests")
    class ReserveStockTests {

        @Test
        @DisplayName("Should reserve stock and return updated response")
        void shouldReserveStockSuccessfully() {
            Inventory inventory = new Inventory(1L, 100, "Warehouse A");
            when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));
            when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));

            InventoryResponse response = inventoryService.reserveStock(1L, 25);

            assertNotNull(response);
            assertEquals(25, response.reservedQuantity());
            assertEquals(75, response.availableQuantity());
            verify(inventoryRepository, times(1)).save(inventory);
        }

        @Test
        @DisplayName("Should throw exception when reserving stock for non-existing product")
        void shouldThrowWhenReservingNonExistingProduct() {
            when(inventoryRepository.findByProductId(999L)).thenReturn(Optional.empty());

            assertThrows(
                    IllegalArgumentException.class,
                    () -> inventoryService.reserveStock(999L, 10)
            );

            verify(inventoryRepository, never()).save(any(Inventory.class));
        }

        @Test
        @DisplayName("Should throw exception when reserve quantity exceeds available stock")
        void shouldThrowWhenReservingMoreThanAvailable() {
            Inventory inventory = new Inventory(1L, 50, "Warehouse A");
            when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));

            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> inventoryService.reserveStock(1L, 60)
            );

            assertEquals("Insufficient available stock", ex.getMessage());
            verify(inventoryRepository, never()).save(any(Inventory.class));
        }
    }
}
