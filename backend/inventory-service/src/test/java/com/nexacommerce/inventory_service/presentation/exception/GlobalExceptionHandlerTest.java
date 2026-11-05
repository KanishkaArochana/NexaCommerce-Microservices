package com.nexacommerce.inventory_service.presentation.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GlobalExceptionHandler Unit Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("Should return 404 NOT_FOUND when message contains 'not found'")
    void shouldReturn404WhenNotFound() {
        IllegalArgumentException ex = new IllegalArgumentException("Inventory not found for product: 5");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleIllegalArgumentException(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().status());
        assertEquals("Not Found", response.getBody().error());
        assertEquals("Inventory not found for product: 5", response.getBody().message());
        assertNull(response.getBody().validationErrors());
    }

    @Test
    @DisplayName("Should return 409 CONFLICT when message contains 'already exists'")
    void shouldReturn409WhenAlreadyExists() {
        IllegalArgumentException ex = new IllegalArgumentException("Inventory already exists for this product");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleIllegalArgumentException(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().status());
        assertEquals("Conflict", response.getBody().error());
        assertEquals("Inventory already exists for this product", response.getBody().message());
    }

    @Test
    @DisplayName("Should return 400 BAD_REQUEST for general illegal argument exceptions")
    void shouldReturn400ForGeneralIllegalArgument() {
        IllegalArgumentException ex = new IllegalArgumentException("Insufficient available stock");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleIllegalArgumentException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().status());
        assertEquals("Bad Request", response.getBody().error());
        assertEquals("Insufficient available stock", response.getBody().message());
    }

    @Test
    @DisplayName("Should return 500 INTERNAL_SERVER_ERROR for unhandled general exceptions")
    void shouldReturn500ForGeneralException() {
        RuntimeException ex = new RuntimeException("Database timeout error");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGeneralException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().status());
        assertEquals("Internal Server Error", response.getBody().error());
        assertTrue(response.getBody().message().contains("Database timeout error"));
    }
}
