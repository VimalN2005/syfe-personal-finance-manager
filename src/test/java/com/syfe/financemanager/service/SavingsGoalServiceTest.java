package com.syfe.financemanager.service;

import com.syfe.financemanager.common.BadRequestException;
import com.syfe.financemanager.common.ForbiddenException;
import com.syfe.financemanager.common.ResourceNotFoundException;
import com.syfe.financemanager.dto.auth.MessageResponse;
import com.syfe.financemanager.dto.goal.GoalListResponse;
import com.syfe.financemanager.dto.goal.GoalRequest;
import com.syfe.financemanager.dto.goal.GoalResponse;
import com.syfe.financemanager.dto.goal.GoalUpdateRequest;
import com.syfe.financemanager.entity.*;
import com.syfe.financemanager.repository.SavingsGoalRepository;
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
class SavingsGoalServiceTest {

    @Mock
    private SavingsGoalRepository goalRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private SavingsGoalServiceImpl goalService;

    private User user;
    private User otherUser;
    private SavingsGoal goal;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).username("user@example.com").build();
        otherUser = User.builder().id(2L).username("other@example.com").build();
        goal = SavingsGoal.builder()
                .id(1L)
                .goalName("Emergency Fund")
                .targetAmount(new BigDecimal("10000.00"))
                .targetDate(LocalDate.now().plusYears(1))
                .startDate(LocalDate.of(2024, 1, 1))
                .user(user)
                .build();
    }

    @Test
    void testCreateGoal_SuccessWithCalculations() {
        GoalRequest request = GoalRequest.builder()
                .goalName("Emergency Fund")
                .targetAmount(new BigDecimal("10000.00"))
                .targetDate(LocalDate.now().plusYears(1))
                .startDate(LocalDate.of(2024, 1, 1))
                .build();

        Category incomeCat = Category.builder().name("Salary").type(TransactionType.INCOME).build();
        Category expenseCat = Category.builder().name("Food").type(TransactionType.EXPENSE).build();

        Transaction incomeTx = Transaction.builder().amount(new BigDecimal("7000.00")).category(incomeCat).build();
        Transaction expenseTx = Transaction.builder().amount(new BigDecimal("450.00")).category(expenseCat).build();

        when(goalRepository.save(any(SavingsGoal.class))).thenReturn(goal);
        when(transactionRepository.findByUserAndDateGreaterThanEqual(eq(user), any(LocalDate.class)))
                .thenReturn(List.of(incomeTx, expenseTx));

        GoalResponse response = goalService.createGoal(request, user);

        assertThat(response.getCurrentProgress()).isEqualByComparingTo("6550.00");
        assertThat(response.getProgressPercentage()).isEqualTo(65.5);
        assertThat(response.getRemainingAmount()).isEqualByComparingTo("3450.00");
    }

    @Test
    void testCreateGoal_PastTargetDateThrowsBadRequest() {
        GoalRequest request = GoalRequest.builder()
                .goalName("Invalid Goal")
                .targetAmount(new BigDecimal("5000.00"))
                .targetDate(LocalDate.of(2023, 1, 1))
                .build();

        assertThatThrownBy(() -> goalService.createGoal(request, user))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("future");
    }

    @Test
    void testCreateGoal_StartDateAfterTargetDateThrowsBadRequest() {
        GoalRequest request = GoalRequest.builder()
                .goalName("Invalid Dates Goal")
                .targetAmount(new BigDecimal("5000.00"))
                .targetDate(LocalDate.now().plusDays(10))
                .startDate(LocalDate.now().plusDays(20))
                .build();

        assertThatThrownBy(() -> goalService.createGoal(request, user))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("after");
    }

    @Test
    void testGetAllGoals() {
        when(goalRepository.findByUserOrderByIdAsc(user)).thenReturn(List.of(goal));
        when(transactionRepository.findByUserAndDateGreaterThanEqual(eq(user), any(LocalDate.class)))
                .thenReturn(List.of());

        GoalListResponse response = goalService.getAllGoals(user);

        assertThat(response.getGoals()).hasSize(1);
        assertThat(response.getGoals().get(0).getCurrentProgress()).isEqualByComparingTo("0.00");
        assertThat(response.getGoals().get(0).getProgressPercentage()).isEqualTo(0.0);
    }

    @Test
    void testUpdateGoal_Success() {
        GoalUpdateRequest request = GoalUpdateRequest.builder()
                .targetAmount(new BigDecimal("15000.00"))
                .build();

        when(goalRepository.findById(1L)).thenReturn(Optional.of(goal));
        when(goalRepository.save(any(SavingsGoal.class))).thenReturn(goal);
        when(transactionRepository.findByUserAndDateGreaterThanEqual(eq(user), any(LocalDate.class)))
                .thenReturn(List.of());

        GoalResponse response = goalService.updateGoal(1L, request, user);

        assertThat(goal.getTargetAmount()).isEqualByComparingTo("15000.00");
    }

    @Test
    void testDeleteGoal_Success() {
        when(goalRepository.findById(1L)).thenReturn(Optional.of(goal));

        MessageResponse response = goalService.deleteGoal(1L, user);

        assertThat(response.getMessage()).isEqualTo("Goal deleted successfully");
        verify(goalRepository).delete(goal);
    }

    @Test
    void testDeleteGoal_OtherUserThrowsForbidden() {
        when(goalRepository.findById(1L)).thenReturn(Optional.of(goal));

        assertThatThrownBy(() -> goalService.deleteGoal(1L, otherUser))
                .isInstanceOf(ForbiddenException.class);
    }
}
