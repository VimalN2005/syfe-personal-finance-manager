package com.syfe.financemanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syfe.financemanager.dto.auth.MessageResponse;
import com.syfe.financemanager.dto.transaction.TransactionListResponse;
import com.syfe.financemanager.dto.transaction.TransactionRequest;
import com.syfe.financemanager.dto.transaction.TransactionResponse;
import com.syfe.financemanager.dto.transaction.TransactionUpdateRequest;
import com.syfe.financemanager.entity.TransactionType;
import com.syfe.financemanager.entity.User;
import com.syfe.financemanager.service.AuthService;
import com.syfe.financemanager.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionService transactionService;

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
    void testCreateTransaction() throws Exception {
        TransactionRequest request = TransactionRequest.builder()
                .amount(new BigDecimal("5000.00"))
                .date(LocalDate.of(2024, 1, 15))
                .category("Salary")
                .description("January Salary")
                .build();

        TransactionResponse response = TransactionResponse.builder()
                .id(1L)
                .amount(new BigDecimal("5000.00"))
                .date(LocalDate.of(2024, 1, 15))
                .category("Salary")
                .description("January Salary")
                .type(TransactionType.INCOME)
                .build();

        when(transactionService.createTransaction(any(), eq(mockUser))).thenReturn(response);

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.amount").value(5000.00))
                .andExpect(jsonPath("$.type").value("INCOME"));
    }

    @Test
    @WithMockUser
    void testGetTransactions() throws Exception {
        TransactionResponse tx = TransactionResponse.builder()
                .id(1L)
                .amount(new BigDecimal("5000.00"))
                .date(LocalDate.of(2024, 1, 15))
                .category("Salary")
                .type(TransactionType.INCOME)
                .build();

        when(transactionService.getTransactions(eq(mockUser), any(), any(), any(), any(), any()))
                .thenReturn(TransactionListResponse.builder().transactions(List.of(tx)).build());

        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions[0].id").value(1));
    }

    @Test
    @WithMockUser
    void testUpdateTransaction() throws Exception {
        TransactionUpdateRequest request = TransactionUpdateRequest.builder()
                .amount(new BigDecimal("6000.00"))
                .description("Updated Salary")
                .build();

        TransactionResponse response = TransactionResponse.builder()
                .id(1L)
                .amount(new BigDecimal("6000.00"))
                .date(LocalDate.of(2024, 1, 15))
                .category("Salary")
                .description("Updated Salary")
                .type(TransactionType.INCOME)
                .build();

        when(transactionService.updateTransaction(eq(1L), any(), eq(mockUser))).thenReturn(response);

        mockMvc.perform(put("/api/transactions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(6000.00));
    }

    @Test
    @WithMockUser
    void testDeleteTransaction() throws Exception {
        when(transactionService.deleteTransaction(1L, mockUser))
                .thenReturn(new MessageResponse("Transaction deleted successfully"));

        mockMvc.perform(delete("/api/transactions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Transaction deleted successfully"));
    }
}
