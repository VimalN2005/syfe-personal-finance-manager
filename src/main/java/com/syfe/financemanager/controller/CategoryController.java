package com.syfe.financemanager.controller;

import com.syfe.financemanager.dto.auth.MessageResponse;
import com.syfe.financemanager.dto.category.CategoryListResponse;
import com.syfe.financemanager.dto.category.CategoryRequest;
import com.syfe.financemanager.dto.category.CategoryResponse;
import com.syfe.financemanager.entity.User;
import com.syfe.financemanager.service.AuthService;
import com.syfe.financemanager.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/categories")
@Tag(name = "Category Management", description = "CRUD operations for categories")
public class CategoryController {

    private final CategoryService categoryService;
    private final AuthService authService;

    public CategoryController(CategoryService categoryService, AuthService authService) {
        this.categoryService = categoryService;
        this.authService = authService;
    }

    @GetMapping
    @Operation(summary = "Get all accessible categories (default and custom)")
    public ResponseEntity<CategoryListResponse> getAllCategories() {
        User currentUser = authService.getCurrentAuthenticatedUser();
        return ResponseEntity.ok(categoryService.getAllCategories(currentUser));
    }

    @PostMapping
    @Operation(summary = "Create a custom category")
    public ResponseEntity<CategoryResponse> createCustomCategory(@Valid @RequestBody CategoryRequest request) {
        User currentUser = authService.getCurrentAuthenticatedUser();
        CategoryResponse response = categoryService.createCustomCategory(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{name}")
    @Operation(summary = "Delete a custom category by name")
    public ResponseEntity<MessageResponse> deleteCustomCategory(@PathVariable String name) {
        User currentUser = authService.getCurrentAuthenticatedUser();
        MessageResponse response = categoryService.deleteCustomCategory(name, currentUser);
        return ResponseEntity.ok(response);
    }
}
