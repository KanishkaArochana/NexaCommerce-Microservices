package com.nexacommerce.product_service.application.service;

import com.nexacommerce.product_service.application.dto.ProductRequest;
import com.nexacommerce.product_service.application.dto.ProductResponse;
import com.nexacommerce.product_service.domain.model.Category;
import com.nexacommerce.product_service.domain.model.Product;
import com.nexacommerce.product_service.infrastructure.persistence.CategoryRepository;
import com.nexacommerce.product_service.infrastructure.persistence.ProductRepository;
import com.nexacommerce.product_service.presentation.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public ProductResponse createProduct(ProductRequest request) {
        Category category = categoryRepository.findById(
                request.categoryId()
        ).orElseThrow(() -> new ResourceNotFoundException(
                "Category not found: " + request.categoryId()
        ));

        Product product = new Product(
                request.name(),
                request.description(),
                request.brand(),
                request.price(),
                request.imageUrl(),
                category
        );

        return toResponse(productRepository.save(product));
    }

    public ProductResponse create(ProductRequest request) {
        return createProduct(request);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> findAllProduct(
            Long categoryId,
            String search
    ) {
        List<Product> products;

        if (categoryId != null) {
            products = productRepository
                    .findByCategoryIdAndActiveTrue(categoryId);
        } else if (search != null && !search.isBlank()) {
            products = productRepository
                    .findByNameContainingIgnoreCaseAndActiveTrue(search.trim());
        } else {
            products = productRepository.findByActiveTrue();
        }

        return products.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> findAll(Long categoryId, String search) {
        return findAllProduct(categoryId, search);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProduct() {
        return findAllProduct(null, null);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAll() {
        return getAllProduct();
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> searchProduct(String name) {
        if (name == null || name.isBlank()) {
            return getAllProduct();
        }

        return productRepository
                .findByNameContainingIgnoreCaseAndActiveTrue(name.trim())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> search(String name) {
        return searchProduct(name);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getProductByCategory(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException(
                    "Category not found: " + categoryId
            );
        }

        return productRepository
                .findByCategoryIdAndActiveTrue(categoryId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getByCategory(Long categoryId) {
        return getProductByCategory(categoryId);
    }

    @Transactional(readOnly = true)
    public ProductResponse findByIdProduct(Long id) {
        Product product = productRepository.findById(id)
                .filter(Product::isActive)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found: " + id
                ));

        return toResponse(product);
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        return findByIdProduct(id);
    }

    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .filter(Product::isActive)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found: " + id
                ));

        Category category = categoryRepository.findById(
                request.categoryId()
        ).orElseThrow(() -> new ResourceNotFoundException(
                "Category not found: " + request.categoryId()
        ));

        product.updateDetailsProduct(
                request.name(),
                request.description(),
                request.brand(),
                request.price(),
                request.imageUrl(),
                category
        );

        return toResponse(productRepository.save(product));
    }

    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found: " + id
                ));

        product.deactivate();
        productRepository.save(product);
    }

    public void deleteProctuct(Long id) {
        deleteProduct(id);
    }

    public void deactivateProduct(Long id) {
        deleteProduct(id);
    }

    public void deactivate(Long id) {
        deleteProduct(id);
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getBrand(),
                product.getPrice(),
                product.getImageUrl(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.isActive()
        );
    }
}