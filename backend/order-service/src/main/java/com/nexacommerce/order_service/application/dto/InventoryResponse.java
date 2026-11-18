package com.nexacommerce.order_service.application.dto;

public record InventoryResponse(
        Long id,
        Long productId,
        int stockQuantity,
        int reservedQuantity,
        int availableQuantity
) {
}