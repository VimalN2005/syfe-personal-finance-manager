package com.syfe.financemanager.service;

import com.syfe.financemanager.common.BadRequestException;
import com.syfe.financemanager.common.ConflictException;
import com.syfe.financemanager.common.ResourceNotFoundException;
import com.syfe.financemanager.dto.auth.MessageResponse;
import com.syfe.financemanager.dto.category.CategoryListResponse;
import com.syfe.financemanager.dto.category.CategoryRequest;
import com.syfe.financemanager.dto.category.CategoryResponse;
import com.syfe.financemanager.entity.Category;
import com.syfe.financemanager.entity.User;
import com.syfe.financemanager.repository.CategoryRepository;
import com.syfe.financemanager.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository,
                               TransactionRepository transactionRepository) {
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryListResponse getAllCategories(User user) {
        List<CategoryResponse> categories = categoryRepository.findAllAccessibleByUser(user)
                .stream()
                .map(this::mapToResponse)
                .toList();
        return CategoryListResponse.builder().categories(categories).build();
    }

    @Override
    @Transactional
    public CategoryResponse createCustomCategory(CategoryRequest request, User user) {
        String name = request.getName().trim();

        if (categoryRepository.existsByNameIgnoreCaseAndUserIsNull(name) ||
                categoryRepository.existsByNameIgnoreCaseAndUser(name, user)) {
            throw new ConflictException("Category with name '" + name + "' already exists");
        }

        Category category = Category.builder()
                .name(name)
                .type(request.getType())
                .isCustom(true)
                .user(user)
                .build();

        Category savedCategory = categoryRepository.save(category);
        return mapToResponse(savedCategory);
    }

    @Override
    @Transactional
    public MessageResponse deleteCustomCategory(String name, User user) {
        String trimmedName = name.trim();

        // Prevent deletion of default categories
        if (categoryRepository.findByNameIgnoreCaseAndUserIsNull(trimmedName).isPresent()) {
            throw new BadRequestException("Default categories cannot be deleted or modified");
        }

        Category category = categoryRepository.findByNameIgnoreCaseAndUser(trimmedName, user)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + trimmedName));

        // Check if category is currently referenced by transactions
        if (transactionRepository.existsByCategory(category)) {
            throw new BadRequestException("Categories currently referenced by transactions cannot be deleted");
        }

        categoryRepository.delete(category);

        return MessageResponse.builder()
                .message("Category deleted successfully")
                .build();
    }

    private CategoryResponse mapToResponse(Category category) {
        return CategoryResponse.builder()
                .name(category.getName())
                .type(category.getType())
                .isCustom(category.isCustom())
                .build();
    }
}
