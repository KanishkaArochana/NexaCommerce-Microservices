package com.nexacommerce.product_service.application.dto;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String name,
        String description,
        String brand,
        BigDecimal price,
        String imageUrl,
        Long categoryId,
        String categoryName,
        boolean active
) {
}