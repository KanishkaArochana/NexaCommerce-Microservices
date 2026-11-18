package com.nexacommerce.order_service.infrastructure.client;

import com.nexacommerce.order_service.application.dto.InventoryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "inventory-service", url = "${INVENTORY_SERVICE_URL:http://localhost:8083}")
public interface InventoryClient {

    // 1. Check product stock
    @GetMapping("/api/v1/inventory/{productId}")
    InventoryResponse getInventoryByProductId(@PathVariable("productId") Long productId);

    // 2. Reserve stock for the order
    @PostMapping("/api/v1/inventory/{productId}/reserve")
    InventoryResponse reserveStock(
            @PathVariable("productId") Long productId,
            @RequestParam("quantity") int quantity
    );
}