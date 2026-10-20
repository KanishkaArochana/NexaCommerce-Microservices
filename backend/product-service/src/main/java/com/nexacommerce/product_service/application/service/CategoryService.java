package com.nexacommerce.product_service.application.service;

import com.nexacommerce.product_service.application.dto.CategoryRequest;
import com.nexacommerce.product_service.application.dto.CategoryResponse;
import com.nexacommerce.product_service.domain.model.Category;
import com.nexacommerce.product_service.infrastructure.persistence.CategoryRepository;
import com.nexacommerce.product_service.presentation.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public CategoryResponse createCategory(CategoryRequest request) {
        categoryRepository.findByNameIgnoreCase(request.name().trim())
                .ifPresent(category -> {
                    throw new IllegalArgumentException(
                            "Category name already exists"
                    );
                });

        Category category = new Category(
                request.name().trim(),
                request.description()
        );

        return toResponse(categoryRepository.save(category));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> findAllCategory() {
        return categoryRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAll() {
        return findAllCategory();
    }

    @Transactional(readOnly = true)
    public CategoryResponse findByIdCategory(Long id) {
        return toResponse(findCategory(id));
    }

    @Transactional(readOnly = true)
    public CategoryResponse getById(Long id) {
        return findByIdCategory(id);
    }

    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = findCategory(id);

        categoryRepository.findByNameIgnoreCase(request.name().trim())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            "Category name already exists"
                    );
                });

        category.updateDetailsCategory(
                request.name().trim(),
                request.description()
        );

        return toResponse(categoryRepository.save(category));
    }

    public CategoryResponse update(Long id, CategoryRequest request) {
        return updateCategory(id, request);
    }

    public void deleteCategory(Long id) {
        Category category = findCategory(id);
        categoryRepository.delete(category);
    }

    public void delete(Long id) {
        deleteCategory(id);
    }

    private Category findCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found: " + id
                ));
    }

    public CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription()
        );
    }
}