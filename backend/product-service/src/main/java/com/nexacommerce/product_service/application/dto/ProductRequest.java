package com.nexacommerce.product_service.application.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank
        @Size(max = 150)
        String name,

        @Size(max = 2000)
        String description,

        @Size(max = 100)
        String brand,

        @NotNull
        @DecimalMin(value = "0.01")
        @Digits(integer = 10, fraction = 2)
        BigDecimal price,

        @Size(max = 1000)
        String imageUrl,

        @NotNull
        @Positive
        Long categoryId
) {
}