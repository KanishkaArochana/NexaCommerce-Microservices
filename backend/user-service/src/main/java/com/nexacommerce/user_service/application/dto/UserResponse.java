package com.nexacommerce.user_service.application.dto;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String role,
        boolean active,
        LocalDateTime createdAt
) {
}