package com.nexacommerce.inventory_service.application.service;

import com.nexacommerce.inventory_service.application.dto.InventoryRequest;
import com.nexacommerce.inventory_service.application.dto.InventoryResponse;
import com.nexacommerce.inventory_service.domain.model.Inventory;
import com.nexacommerce.inventory_service.infrastructure.persistence.InventoryRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional
    public InventoryResponse create(InventoryRequest request) {
        if (inventoryRepository.findByProductId(request.productId())
                .isPresent()) {
            throw new IllegalArgumentException(
                    "Inventory already exists for this product"
            );
        }

        Inventory inventory = new Inventory(
                request.productId(),
                request.stockQuantity(),
                request.warehouseLocation().trim()
        );

        return toResponse(inventoryRepository.save(inventory));
    }

    @Transactional(readOnly = true)
    public InventoryResponse getByProductId(Long productId) {
        Inventory inventory = inventoryRepository
                .findByProductId(productId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Inventory not found for product: " + productId
                ));

        return toResponse(inventory);
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> getAll() {
        return inventoryRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public InventoryResponse updateStock(Long productId, int quantity) {
        Inventory inventory = inventoryRepository
                .findByProductId(productId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Inventory not found for product: " + productId
                ));

        inventory.updateStock(quantity);

        return toResponse(inventoryRepository.save(inventory));
    }

    @Transactional
    public InventoryResponse reserveStock(Long productId, int quantity) {
        Inventory inventory = inventoryRepository
                .findByProductId(productId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Inventory not found for product: " + productId
                ));

        inventory.reserveStock(quantity);

        return toResponse(inventoryRepository.save(inventory));
    }

    private InventoryResponse toResponse(Inventory inventory) {
        return new InventoryResponse(
                inventory.getId(),
                inventory.getProductId(),
                inventory.getStockQuantity(),
                inventory.getReservedQuantity(),
                inventory.getAvailableQuantity(),
                inventory.getWarehouseLocation(),
                inventory.getUpdatedAt()
        );
    }
}