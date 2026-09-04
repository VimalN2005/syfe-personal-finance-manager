package com.syfe.financemanager.service;

import com.syfe.financemanager.common.BadRequestException;
import com.syfe.financemanager.common.ConflictException;
import com.syfe.financemanager.common.ResourceNotFoundException;
import com.syfe.financemanager.dto.auth.MessageResponse;
import com.syfe.financemanager.dto.category.CategoryListResponse;
import com.syfe.financemanager.dto.category.CategoryRequest;
import com.syfe.financemanager.dto.category.CategoryResponse;
import com.syfe.financemanager.entity.Category;
import com.syfe.financemanager.entity.TransactionType;
import com.syfe.financemanager.entity.User;
import com.syfe.financemanager.repository.CategoryRepository;
import com.syfe.financemanager.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private User user;
    private Category defaultCategory;
    private Category customCategory;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).username("user@example.com").build();
        defaultCategory = Category.builder().id(1L).name("Salary").type(TransactionType.INCOME).isCustom(false).build();
        customCategory = Category.builder().id(2L).name("Freelance").type(TransactionType.INCOME).isCustom(true).user(user).build();
    }

    @Test
    void testGetAllCategories() {
        when(categoryRepository.findAllAccessibleByUser(user)).thenReturn(List.of(defaultCategory, customCategory));

        CategoryListResponse response = categoryService.getAllCategories(user);

        assertThat(response.getCategories()).hasSize(2);
        assertThat(response.getCategories().get(0).getName()).isEqualTo("Salary");
        assertThat(response.getCategories().get(0).isCustom()).isFalse();
        assertThat(response.getCategories().get(1).getName()).isEqualTo("Freelance");
        assertThat(response.getCategories().get(1).isCustom()).isTrue();
    }

    @Test
    void testCreateCustomCategory_Success() {
        CategoryRequest request = CategoryRequest.builder().name("SideBiz").type(TransactionType.INCOME).build();

        when(categoryRepository.existsByNameIgnoreCaseAndUserIsNull("SideBiz")).thenReturn(false);
        when(categoryRepository.existsByNameIgnoreCaseAndUser("SideBiz", user)).thenReturn(false);

        Category saved = Category.builder().id(3L).name("SideBiz").type(TransactionType.INCOME).isCustom(true).user(user).build();
        when(categoryRepository.save(any(Category.class))).thenReturn(saved);

        CategoryResponse response = categoryService.createCustomCategory(request, user);

        assertThat(response.getName()).isEqualTo("SideBiz");
        assertThat(response.isCustom()).isTrue();
        assertThat(response.getCustom()).isTrue();
    }

    @Test
    void testCreateCustomCategory_DuplicateThrowsConflict() {
        CategoryRequest request = CategoryRequest.builder().name("Salary").type(TransactionType.INCOME).build();

        when(categoryRepository.existsByNameIgnoreCaseAndUserIsNull("Salary")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.createCustomCategory(request, user))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void testDeleteCustomCategory_Success() {
        when(categoryRepository.findByNameIgnoreCaseAndUserIsNull("Freelance")).thenReturn(Optional.empty());
        when(categoryRepository.findByNameIgnoreCaseAndUser("Freelance", user)).thenReturn(Optional.of(customCategory));
        when(transactionRepository.existsByCategory(customCategory)).thenReturn(false);

        MessageResponse response = categoryService.deleteCustomCategory("Freelance", user);

        assertThat(response.getMessage()).isEqualTo("Category deleted successfully");
        verify(categoryRepository).delete(customCategory);
    }

    @Test
    void testDeleteCustomCategory_DefaultCategoryThrowsBadRequest() {
        when(categoryRepository.findByNameIgnoreCaseAndUserIsNull("Salary")).thenReturn(Optional.of(defaultCategory));

        assertThatThrownBy(() -> categoryService.deleteCustomCategory("Salary", user))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Default categories cannot be deleted");
    }

    @Test
    void testDeleteCustomCategory_InUseThrowsBadRequest() {
        when(categoryRepository.findByNameIgnoreCaseAndUserIsNull("Freelance")).thenReturn(Optional.empty());
        when(categoryRepository.findByNameIgnoreCaseAndUser("Freelance", user)).thenReturn(Optional.of(customCategory));
        when(transactionRepository.existsByCategory(customCategory)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.deleteCustomCategory("Freelance", user))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Categories currently referenced by transactions cannot be deleted");
    }

    @Test
    void testDeleteCustomCategory_NotFound() {
        when(categoryRepository.findByNameIgnoreCaseAndUserIsNull("Unknown")).thenReturn(Optional.empty());
        when(categoryRepository.findByNameIgnoreCaseAndUser("Unknown", user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.deleteCustomCategory("Unknown", user))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
