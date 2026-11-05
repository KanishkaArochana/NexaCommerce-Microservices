package com.nexacommerce.inventory_service.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record InventoryRequest(
        @NotNull
        @Positive
        Long productId,

        @Min(0)
        int stockQuantity,

        @NotBlank
        String warehouseLocation
) {
}