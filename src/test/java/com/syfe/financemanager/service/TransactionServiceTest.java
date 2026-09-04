package com.syfe.financemanager.service;

import com.syfe.financemanager.common.BadRequestException;
import com.syfe.financemanager.common.ForbiddenException;
import com.syfe.financemanager.common.ResourceNotFoundException;
import com.syfe.financemanager.dto.auth.MessageResponse;
import com.syfe.financemanager.dto.transaction.TransactionListResponse;
import com.syfe.financemanager.dto.transaction.TransactionRequest;
import com.syfe.financemanager.dto.transaction.TransactionResponse;
import com.syfe.financemanager.dto.transaction.TransactionUpdateRequest;
import com.syfe.financemanager.entity.Category;
import com.syfe.financemanager.entity.Transaction;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private User user;
    private User otherUser;
    private Category category;
    private Transaction transaction;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).username("user@example.com").build();
        otherUser = User.builder().id(2L).username("other@example.com").build();
        category = Category.builder().id(1L).name("Salary").type(TransactionType.INCOME).isCustom(false).build();
        transaction = Transaction.builder()
                .id(10L)
                .amount(new BigDecimal("5000.00"))
                .date(LocalDate.of(2024, 1, 15))
                .category(category)
                .description("Salary payment")
                .user(user)
                .build();
    }

    @Test
    void testCreateTransaction_Success() {
        TransactionRequest request = TransactionRequest.builder()
                .amount(new BigDecimal("5000.00"))
                .date(LocalDate.of(2024, 1, 15))
                .category("Salary")
                .description("Salary payment")
                .build();

        when(categoryRepository.findByNameAccessibleByUser("Salary", user)).thenReturn(Optional.of(category));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);

        TransactionResponse response = transactionService.createTransaction(request, user);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getAmount()).isEqualByComparingTo("5000.00");
        assertThat(response.getType()).isEqualTo(TransactionType.INCOME);
    }

    @Test
    void testCreateTransaction_FutureDateThrowsBadRequest() {
        TransactionRequest request = TransactionRequest.builder()
                .amount(new BigDecimal("100.00"))
                .date(LocalDate.now().plusDays(10))
                .category("Salary")
                .build();

        assertThatThrownBy(() -> transactionService.createTransaction(request, user))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("future");
    }

    @Test
    void testCreateTransaction_InvalidCategoryThrowsBadRequest() {
        TransactionRequest request = TransactionRequest.builder()
                .amount(new BigDecimal("100.00"))
                .date(LocalDate.of(2024, 1, 15))
                .category("InvalidCategory")
                .build();

        when(categoryRepository.findByNameAccessibleByUser("InvalidCategory", user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.createTransaction(request, user))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not accessible");
    }

    @Test
    void testGetTransactions_WithFilters() {
        when(transactionRepository.findByUserOrderByDateDescIdDesc(user)).thenReturn(List.of(transaction));

        TransactionListResponse response = transactionService.getTransactions(
                user,
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31),
                "Salary",
                1L,
                TransactionType.INCOME
        );

        assertThat(response.getTransactions()).hasSize(1);
    }

    @Test
    void testUpdateTransaction_DateIsImmutable() {
        TransactionUpdateRequest request = TransactionUpdateRequest.builder()
                .amount(new BigDecimal("6000.00"))
                .date(LocalDate.of(2024, 2, 20)) // Attempted date change
                .description("Updated Salary")
                .build();

        when(transactionRepository.findById(10L)).thenReturn(Optional.of(transaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);

        TransactionResponse response = transactionService.updateTransaction(10L, request, user);

        // Verify date remained original 2024-01-15
        assertThat(transaction.getDate()).isEqualTo(LocalDate.of(2024, 1, 15));
        assertThat(transaction.getAmount()).isEqualByComparingTo("6000.00");
    }

    @Test
    void testUpdateTransaction_OtherUserThrowsForbidden() {
        TransactionUpdateRequest request = TransactionUpdateRequest.builder().amount(new BigDecimal("1000.00")).build();
        when(transactionRepository.findById(10L)).thenReturn(Optional.of(transaction));

        assertThatThrownBy(() -> transactionService.updateTransaction(10L, request, otherUser))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void testDeleteTransaction_Success() {
        when(transactionRepository.findById(10L)).thenReturn(Optional.of(transaction));

        MessageResponse response = transactionService.deleteTransaction(10L, user);

        assertThat(response.getMessage()).isEqualTo("Transaction deleted successfully");
        verify(transactionRepository).delete(transaction);
    }

    @Test
    void testDeleteTransaction_OtherUserThrowsForbidden() {
        when(transactionRepository.findById(10L)).thenReturn(Optional.of(transaction));

        assertThatThrownBy(() -> transactionService.deleteTransaction(10L, otherUser))
                .isInstanceOf(ForbiddenException.class);
    }
}
