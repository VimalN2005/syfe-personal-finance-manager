package com.syfe.financemanager.service;

import com.syfe.financemanager.dto.auth.MessageResponse;
import com.syfe.financemanager.dto.category.CategoryListResponse;
import com.syfe.financemanager.dto.category.CategoryRequest;
import com.syfe.financemanager.dto.category.CategoryResponse;
import com.syfe.financemanager.entity.User;

public interface CategoryService {
    CategoryListResponse getAllCategories(User user);
    CategoryResponse createCustomCategory(CategoryRequest request, User user);
    MessageResponse deleteCustomCategory(String name, User user);
}
