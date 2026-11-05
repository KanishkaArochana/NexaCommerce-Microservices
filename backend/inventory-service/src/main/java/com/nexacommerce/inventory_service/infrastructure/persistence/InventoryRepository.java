package com.nexacommerce.inventory_service.infrastructure.persistence;

import com.nexacommerce.inventory_service.domain.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InventoryRepository
        extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductId(Long productId);
}