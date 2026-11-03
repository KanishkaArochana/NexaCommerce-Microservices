package com.nexacommerce.product_service.presentation.controller;

import com.nexacommerce.product_service.application.dto.ProductRequest;
import com.nexacommerce.product_service.application.dto.ProductResponse;
import com.nexacommerce.product_service.application.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<ProductResponse> findAllProduct(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String search
    ) {
        if (categoryId != null && search != null && !search.isBlank()) {
            return productService.findAllProduct(categoryId, null);
        }

        return productService.findAllProduct(categoryId, search);
    }

    @GetMapping("/{id}")
    public ProductResponse findByIdProduct(@PathVariable Long id) {
        return productService.findByIdProduct(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse createProduct(
            @Valid @RequestBody ProductRequest request
    ) {
        return productService.createProduct(request);
    }

    @PutMapping("/{id}")
    public ProductResponse updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request
    ) {
        return productService.updateProduct(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
    }
}