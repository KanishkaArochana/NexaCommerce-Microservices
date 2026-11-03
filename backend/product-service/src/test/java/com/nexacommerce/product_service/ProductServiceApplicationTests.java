package com.nexacommerce.product_service;

import com.nexacommerce.product_service.application.dto.ProductRequest;
import com.nexacommerce.product_service.application.dto.ProductResponse;
import com.nexacommerce.product_service.application.service.ProductService;
import com.nexacommerce.product_service.domain.model.Category;
import com.nexacommerce.product_service.domain.model.Product;
import com.nexacommerce.product_service.infrastructure.persistence.CategoryRepository;
import com.nexacommerce.product_service.infrastructure.persistence.ProductRepository;
import com.nexacommerce.product_service.presentation.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceApplicationTests {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductService productService;

    private Category category;
    private ProductRequest request;

    @BeforeEach
    void setUp() {
        category = new Category("Mobile Phones", "Mobile devices");

        request = new ProductRequest(
                "Samsung Galaxy S25",
                "Samsung smartphone",
                "Samsung",
                new BigDecimal("250000.00"),
                "https://example.com/samsung.jpg",
                1L
        );
    }

    @Test
    void create_ShouldCreateProduct_WhenCategoryExists() {
        // Arrange
        when(categoryRepository.findById(1L))
                .thenReturn(Optional.of(category));

        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ProductResponse response = productService.createProduct(request);

        // Assert
        assertNotNull(response);
        assertEquals("Samsung Galaxy S25", response.name());
        assertEquals("Samsung", response.brand());
        assertEquals(new BigDecimal("250000.00"), response.price());
        assertEquals("Mobile Phones", response.categoryName());

        verify(categoryRepository).findById(1L);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void create_ShouldThrowException_WhenCategoryDoesNotExist() {
        // Arrange
        when(categoryRepository.findById(1L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                ResourceNotFoundException.class,
                () -> productService.createProduct(request)
        );

        verify(productRepository, never()).save(any(Product.class));
    }
}