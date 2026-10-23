package com.nexacommerce.inventory_service.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "inventory",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_inventory_product_id",
                columnNames = "product_id"
        )
)
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity;

    @Column(name = "reserved_quantity", nullable = false)
    private int reservedQuantity = 0;

    @Column(name = "warehouse_location", length = 100)
    private String warehouseLocation;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Inventory() {
    }

    public Inventory(
            Long productId,
            int stockQuantity,
            String warehouseLocation
    ) {
        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("Invalid product ID");
        }

        if (stockQuantity < 0) {
            throw new IllegalArgumentException(
                    "Stock quantity cannot be negative"
            );
        }

        this.productId = productId;
        this.stockQuantity = stockQuantity;
        this.reservedQuantity = 0;
        this.warehouseLocation = warehouseLocation;
        this.updatedAt = LocalDateTime.now();
    }

    public int getAvailableQuantity() {
        return stockQuantity - reservedQuantity;
    }

    public void updateStock(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException(
                    "Stock quantity cannot be negative"
            );
        }

        if (quantity < reservedQuantity) {
            throw new IllegalArgumentException(
                    "Stock cannot be less than reserved quantity"
            );
        }

        this.stockQuantity = quantity;
        this.updatedAt = LocalDateTime.now();
    }

    public void reserveStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "Reservation quantity must be positive"
            );
        }

        if (quantity > getAvailableQuantity()) {
            throw new IllegalArgumentException(
                    "Insufficient available stock"
            );
        }

        this.reservedQuantity += quantity;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getProductId() {
        return productId;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public int getReservedQuantity() {
        return reservedQuantity;
    }

    public String getWarehouseLocation() {
        return warehouseLocation;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
