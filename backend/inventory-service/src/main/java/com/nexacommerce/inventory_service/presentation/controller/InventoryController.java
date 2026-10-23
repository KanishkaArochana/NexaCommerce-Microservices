package com.nexacommerce.inventory_service.presentation.controller;

import com.nexacommerce.inventory_service.application.dto.InventoryRequest;
import com.nexacommerce.inventory_service.application.dto.InventoryResponse;
import com.nexacommerce.inventory_service.application.service.InventoryService;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping
    public InventoryResponse create(
            @Valid @RequestBody InventoryRequest request
    ) {
        return inventoryService.create(request);
    }

    @GetMapping("/{productId}")
    public InventoryResponse getByProductId(
            @PathVariable Long productId
    ) {
        return inventoryService.getByProductId(productId);
    }

    @GetMapping
    public List<InventoryResponse> getAll() {
        return inventoryService.getAll();
    }

    @PutMapping("/{productId}")
    public InventoryResponse updateStock(
            @PathVariable Long productId,
            @RequestParam int quantity
    ) {
        return inventoryService.updateStock(productId, quantity);
    }

    @PostMapping("/{productId}/reserve")
    public InventoryResponse reserveStock(
            @PathVariable Long productId,
            @RequestParam int quantity
    ) {
        return inventoryService.reserveStock(productId, quantity);
    }
}