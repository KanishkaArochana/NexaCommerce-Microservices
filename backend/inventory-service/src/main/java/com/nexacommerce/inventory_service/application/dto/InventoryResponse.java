package com.nexacommerce.inventory_service.application.dto;

import java.time.LocalDateTime;

public record InventoryResponse(
        Long id,
        Long productId,
        int stockQuantity,
        int reservedQuantity,
        int availableQuantity,
        String warehouseLocation,
        LocalDateTime updatedAt
) {
}