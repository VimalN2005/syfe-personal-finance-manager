package com.syfe.financemanager.service;

import com.syfe.financemanager.common.BadRequestException;
import com.syfe.financemanager.dto.report.MonthlyReportResponse;
import com.syfe.financemanager.dto.report.YearlyReportResponse;
import com.syfe.financemanager.entity.Category;
import com.syfe.financemanager.entity.Transaction;
import com.syfe.financemanager.entity.TransactionType;
import com.syfe.financemanager.entity.User;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private ReportServiceImpl reportService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).username("user@example.com").build();
    }

    @Test
    void testGetMonthlyReport_Success() {
        Category salary = Category.builder().name("Salary").type(TransactionType.INCOME).build();
        Category food = Category.builder().name("Food").type(TransactionType.EXPENSE).build();

        Transaction tx1 = Transaction.builder().amount(new BigDecimal("5000.00")).category(salary).build();
        Transaction tx2 = Transaction.builder().amount(new BigDecimal("400.00")).category(food).build();

        when(transactionRepository.findByUserAndDateBetween(eq(user), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(tx1, tx2));

        MonthlyReportResponse response = reportService.getMonthlyReport(2024, 1, user);

        assertThat(response.getMonth()).isEqualTo(1);
        assertThat(response.getYear()).isEqualTo(2024);
        assertThat(response.getTotalIncome()).containsEntry("Salary", new BigDecimal("5000.00"));
        assertThat(response.getTotalExpenses()).containsEntry("Food", new BigDecimal("400.00"));
        assertThat(response.getNetSavings()).isEqualByComparingTo("4600.00");
    }

    @Test
    void testGetMonthlyReport_InvalidMonthThrowsBadRequest() {
        assertThatThrownBy(() -> reportService.getMonthlyReport(2024, 13, user))
                .isInstanceOf(BadRequestException.class);

        assertThatThrownBy(() -> reportService.getMonthlyReport(2024, 0, user))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void testGetYearlyReport_Success() {
        Category salary = Category.builder().name("Salary").type(TransactionType.INCOME).build();
        Transaction tx = Transaction.builder().amount(new BigDecimal("60000.00")).category(salary).build();

        when(transactionRepository.findByUserAndDateBetween(eq(user), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(tx));

        YearlyReportResponse response = reportService.getYearlyReport(2024, user);

        assertThat(response.getYear()).isEqualTo(2024);
        assertThat(response.getTotalIncome()).containsEntry("Salary", new BigDecimal("60000.00"));
        assertThat(response.getNetSavings()).isEqualByComparingTo("60000.00");
    }
}
