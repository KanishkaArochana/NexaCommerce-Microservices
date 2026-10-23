package com.nexacommerce.inventory_service.application.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("InventoryRequest DTO Validation Tests")
class InventoryRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Should pass validation with valid request data")
    void shouldPassValidationWithValidData() {
        InventoryRequest request = new InventoryRequest(1L, 100, "Warehouse A");
        Set<ConstraintViolation<InventoryRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty(), "Valid request should produce no violations");
    }

    @Test
    @DisplayName("Should fail validation when productId is null")
    void shouldFailWhenProductIdIsNull() {
        InventoryRequest request = new InventoryRequest(null, 100, "Warehouse A");
        Set<ConstraintViolation<InventoryRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("productId")));
    }

    @Test
    @DisplayName("Should fail validation when productId is not positive")
    void shouldFailWhenProductIdIsNotPositive() {
        InventoryRequest request = new InventoryRequest(-1L, 100, "Warehouse A");
        Set<ConstraintViolation<InventoryRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("productId")));
    }

    @Test
    @DisplayName("Should fail validation when stockQuantity is negative")
    void shouldFailWhenStockQuantityIsNegative() {
        InventoryRequest request = new InventoryRequest(1L, -5, "Warehouse A");
        Set<ConstraintViolation<InventoryRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("stockQuantity")));
    }

    @Test
    @DisplayName("Should fail validation when warehouseLocation is blank or null")
    void shouldFailWhenWarehouseLocationIsBlankOrNull() {
        InventoryRequest blankRequest = new InventoryRequest(1L, 10, "   ");
        Set<ConstraintViolation<InventoryRequest>> blankViolations = validator.validate(blankRequest);

        assertFalse(blankViolations.isEmpty());
        assertTrue(blankViolations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("warehouseLocation")));

        InventoryRequest nullRequest = new InventoryRequest(1L, 10, null);
        Set<ConstraintViolation<InventoryRequest>> nullViolations = validator.validate(nullRequest);

        assertFalse(nullViolations.isEmpty());
        assertTrue(nullViolations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("warehouseLocation")));
    }
}
