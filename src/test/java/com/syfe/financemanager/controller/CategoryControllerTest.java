package com.syfe.financemanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syfe.financemanager.dto.auth.MessageResponse;
import com.syfe.financemanager.dto.category.CategoryListResponse;
import com.syfe.financemanager.dto.category.CategoryRequest;
import com.syfe.financemanager.dto.category.CategoryResponse;
import com.syfe.financemanager.entity.TransactionType;
import com.syfe.financemanager.entity.User;
import com.syfe.financemanager.service.AuthService;
import com.syfe.financemanager.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CategoryService categoryService;

    @MockBean
    private AuthService authService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.builder().id(1L).username("test@example.com").build();
        when(authService.getCurrentAuthenticatedUser()).thenReturn(mockUser);
    }

    @Test
    @WithMockUser
    void testGetAllCategories() throws Exception {
        CategoryResponse cat1 = CategoryResponse.builder().name("Salary").type(TransactionType.INCOME).isCustom(false).build();
        when(categoryService.getAllCategories(mockUser))
                .thenReturn(CategoryListResponse.builder().categories(List.of(cat1)).build());

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories[0].name").value("Salary"))
                .andExpect(jsonPath("$.categories[0].isCustom").value(false))
                .andExpect(jsonPath("$.categories[0].custom").value(false));
    }

    @Test
    @WithMockUser
    void testCreateCustomCategory() throws Exception {
        CategoryRequest request = CategoryRequest.builder().name("Freelance").type(TransactionType.INCOME).build();
        CategoryResponse response = CategoryResponse.builder().name("Freelance").type(TransactionType.INCOME).isCustom(true).build();

        when(categoryService.createCustomCategory(any(), eq(mockUser))).thenReturn(response);

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Freelance"))
                .andExpect(jsonPath("$.custom").value(true));
    }

    @Test
    @WithMockUser
    void testDeleteCustomCategory() throws Exception {
        when(categoryService.deleteCustomCategory("Freelance", mockUser))
                .thenReturn(new MessageResponse("Category deleted successfully"));

        mockMvc.perform(delete("/api/categories/Freelance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Category deleted successfully"));
    }
}
