package com.nexora.backend.controller;

import com.nexora.backend.entity.Category;
import com.nexora.backend.service.CategoryService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/categories")
@PreAuthorize("hasRole('ADMIN')")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ResponseEntity<List<Category>> getAllCategories() {

        return ResponseEntity.ok(
                categoryService.getAllCategories()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCategoryById(
            @PathVariable Long id
    ) {

        Category category = categoryService
                .getCategoryById(id)
                .orElse(null);

        if (category == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "message",
                            "Category not found"
                    ));
        }

        return ResponseEntity.ok(category);
    }

    @PostMapping
    public ResponseEntity<?> createCategory(
            @Valid @RequestBody CategoryRequest request
    ) {

        String categoryName = request.name().trim();

        boolean exists = categoryService
                .getAllCategories()
                .stream()
                .anyMatch(category ->
                        category.getName()
                                .equalsIgnoreCase(categoryName)
                );

        if (exists) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "message",
                            "Category name already exists"
                    ));
        }

        Category category = new Category();
        category.setName(categoryName);

        Category savedCategory =
                categoryService.saveCategory(category);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(savedCategory);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request
    ) {

        Category category = categoryService
                .getCategoryById(id)
                .orElse(null);

        if (category == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "message",
                            "Category not found"
                    ));
        }

        String categoryName = request.name().trim();

        boolean exists = categoryService
                .getAllCategories()
                .stream()
                .anyMatch(existingCategory ->
                        !existingCategory.getId().equals(id)
                                && existingCategory.getName()
                                .equalsIgnoreCase(categoryName)
                );

        if (exists) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "message",
                            "Category name already exists"
                    ));
        }

        category.setName(categoryName);

        Category updatedCategory =
                categoryService.saveCategory(category);

        return ResponseEntity.ok(updatedCategory);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCategory(
            @PathVariable Long id
    ) {

        Category category = categoryService
                .getCategoryById(id)
                .orElse(null);

        if (category == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "message",
                            "Category not found"
                    ));
        }

        try {

            categoryService.deleteCategory(id);

            return ResponseEntity.noContent().build();

        } catch (Exception e) {

            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "message",
                            "Category cannot be deleted because it is still used by an event"
                    ));
        }
    }

    public record CategoryRequest(
            @NotBlank(message = "Category name is required")
            String name
    ) {
    }
}