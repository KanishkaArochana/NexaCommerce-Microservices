package com.nexacommerce.order_service.infrastructure.client;

import com.nexacommerce.order_service.application.dto.InventoryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class InventoryClientFallbackFactory implements FallbackFactory<InventoryClient> {

    private static final Logger log = LoggerFactory.getLogger(InventoryClientFallbackFactory.class);

    @Override
    public InventoryClient create(Throwable cause) {
        return new InventoryClient() {

            // Fallback when inventory-service is down during stock check
            @Override
            public InventoryResponse getInventoryByProductId(Long productId) {
                log.error("Fallback: Inventory service is unavailable for getInventoryByProductId({}). Reason: {}", productId, cause.getMessage());
                throw new IllegalStateException("Inventory service is currently unavailable. Please try again later.");
            }

            // Fallback when inventory-service is down during stock reservation
            @Override
            public InventoryResponse reserveStock(Long productId, int quantity) {
                log.error("Fallback: Inventory service is unavailable for reserveStock({}, {}). Reason: {}", productId, quantity, cause.getMessage());
                throw new IllegalStateException("Inventory service is currently unavailable. Could not reserve stock.");
            }
        };
    }
}