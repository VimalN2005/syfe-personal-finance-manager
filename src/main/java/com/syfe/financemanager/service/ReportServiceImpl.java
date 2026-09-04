package com.syfe.financemanager.service;

import com.syfe.financemanager.common.BadRequestException;
import com.syfe.financemanager.dto.report.MonthlyReportResponse;
import com.syfe.financemanager.dto.report.YearlyReportResponse;
import com.syfe.financemanager.entity.Transaction;
import com.syfe.financemanager.entity.TransactionType;
import com.syfe.financemanager.entity.User;
import com.syfe.financemanager.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportServiceImpl implements ReportService {

    private final TransactionRepository transactionRepository;

    public ReportServiceImpl(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public MonthlyReportResponse getMonthlyReport(int year, int month, User user) {
        if (month < 1 || month > 12) {
            throw new BadRequestException("Invalid month: " + month + ". Month must be between 1 and 12.");
        }

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        List<Transaction> transactions = transactionRepository.findByUserAndDateBetween(user, startDate, endDate);

        Map<String, BigDecimal> totalIncome = new LinkedHashMap<>();
        Map<String, BigDecimal> totalExpenses = new LinkedHashMap<>();

        BigDecimal sumIncome = BigDecimal.ZERO;
        BigDecimal sumExpenses = BigDecimal.ZERO;

        for (Transaction tx : transactions) {
            String categoryName = tx.getCategory().getName();
            BigDecimal amount = tx.getAmount().setScale(2, RoundingMode.HALF_UP);

            if (tx.getCategory().getType() == TransactionType.INCOME) {
                totalIncome.merge(categoryName, amount, BigDecimal::add);
                sumIncome = sumIncome.add(amount);
            } else {
                totalExpenses.merge(categoryName, amount, BigDecimal::add);
                sumExpenses = sumExpenses.add(amount);
            }
        }

        BigDecimal netSavings = sumIncome.subtract(sumExpenses);

        return MonthlyReportResponse.builder()
                .month(month)
                .year(year)
                .totalIncome(totalIncome)
                .totalExpenses(totalExpenses)
                .netSavings(netSavings)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public YearlyReportResponse getYearlyReport(int year, User user) {
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);

        List<Transaction> transactions = transactionRepository.findByUserAndDateBetween(user, startDate, endDate);

        Map<String, BigDecimal> totalIncome = new LinkedHashMap<>();
        Map<String, BigDecimal> totalExpenses = new LinkedHashMap<>();

        BigDecimal sumIncome = BigDecimal.ZERO;
        BigDecimal sumExpenses = BigDecimal.ZERO;

        for (Transaction tx : transactions) {
            String categoryName = tx.getCategory().getName();
            BigDecimal amount = tx.getAmount().setScale(2, RoundingMode.HALF_UP);

            if (tx.getCategory().getType() == TransactionType.INCOME) {
                totalIncome.merge(categoryName, amount, BigDecimal::add);
                sumIncome = sumIncome.add(amount);
            } else {
                totalExpenses.merge(categoryName, amount, BigDecimal::add);
                sumExpenses = sumExpenses.add(amount);
            }
        }

        BigDecimal netSavings = sumIncome.subtract(sumExpenses);

        return YearlyReportResponse.builder()
                .year(year)
                .totalIncome(totalIncome)
                .totalExpenses(totalExpenses)
                .netSavings(netSavings)
                .build();
    }
}
