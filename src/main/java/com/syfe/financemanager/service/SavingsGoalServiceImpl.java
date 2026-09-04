package com.syfe.financemanager.service;

import com.syfe.financemanager.common.BadRequestException;
import com.syfe.financemanager.common.ForbiddenException;
import com.syfe.financemanager.common.ResourceNotFoundException;
import com.syfe.financemanager.dto.auth.MessageResponse;
import com.syfe.financemanager.dto.goal.GoalListResponse;
import com.syfe.financemanager.dto.goal.GoalRequest;
import com.syfe.financemanager.dto.goal.GoalResponse;
import com.syfe.financemanager.dto.goal.GoalUpdateRequest;
import com.syfe.financemanager.entity.SavingsGoal;
import com.syfe.financemanager.entity.Transaction;
import com.syfe.financemanager.entity.TransactionType;
import com.syfe.financemanager.entity.User;
import com.syfe.financemanager.repository.SavingsGoalRepository;
import com.syfe.financemanager.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class SavingsGoalServiceImpl implements SavingsGoalService {

    private final SavingsGoalRepository goalRepository;
    private final TransactionRepository transactionRepository;

    public SavingsGoalServiceImpl(SavingsGoalRepository goalRepository,
                                  TransactionRepository transactionRepository) {
        this.goalRepository = goalRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional
    public GoalResponse createGoal(GoalRequest request, User user) {
        if (!request.getTargetDate().isAfter(LocalDate.now())) {
            throw new BadRequestException("Target date must be a future date");
        }

        LocalDate startDate = request.getStartDate() != null ? request.getStartDate() : LocalDate.now();

        if (startDate.isAfter(request.getTargetDate())) {
            throw new BadRequestException("Start date cannot be after target date");
        }

        SavingsGoal goal = SavingsGoal.builder()
                .goalName(request.getGoalName().trim())
                .targetAmount(request.getTargetAmount())
                .targetDate(request.getTargetDate())
                .startDate(startDate)
                .user(user)
                .build();

        SavingsGoal saved = goalRepository.save(goal);
        return calculateAndMap(saved, user);
    }

    @Override
    @Transactional(readOnly = true)
    public GoalListResponse getAllGoals(User user) {
        List<GoalResponse> goals = goalRepository.findByUserOrderByIdAsc(user)
                .stream()
                .map(goal -> calculateAndMap(goal, user))
                .toList();
        return GoalListResponse.builder().goals(goals).build();
    }

    @Override
    @Transactional(readOnly = true)
    public GoalResponse getGoalById(Long id, User user) {
        SavingsGoal goal = goalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found with id: " + id));

        if (!goal.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("You do not have permission to view this goal");
        }

        return calculateAndMap(goal, user);
    }

    @Override
    @Transactional
    public GoalResponse updateGoal(Long id, GoalUpdateRequest request, User user) {
        SavingsGoal goal = goalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found with id: " + id));

        if (!goal.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("You do not have permission to modify this goal");
        }

        if (request.getGoalName() != null && !request.getGoalName().isBlank()) {
            goal.setGoalName(request.getGoalName().trim());
        }

        if (request.getTargetAmount() != null) {
            goal.setTargetAmount(request.getTargetAmount());
        }

        if (request.getTargetDate() != null) {
            if (!request.getTargetDate().isAfter(LocalDate.now())) {
                throw new BadRequestException("Target date must be a future date");
            }
            if (goal.getStartDate().isAfter(request.getTargetDate())) {
                throw new BadRequestException("Start date cannot be after target date");
            }
            goal.setTargetDate(request.getTargetDate());
        }

        SavingsGoal updated = goalRepository.save(goal);
        return calculateAndMap(updated, user);
    }

    @Override
    @Transactional
    public MessageResponse deleteGoal(Long id, User user) {
        SavingsGoal goal = goalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found with id: " + id));

        if (!goal.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("You do not have permission to delete this goal");
        }

        goalRepository.delete(goal);
        return MessageResponse.builder().message("Goal deleted successfully").build();
    }

    private GoalResponse calculateAndMap(SavingsGoal goal, User user) {
        List<Transaction> transactions = transactionRepository.findByUserAndDateGreaterThanEqual(user, goal.getStartDate());

        BigDecimal totalIncome = transactions.stream()
                .filter(t -> t.getCategory().getType() == TransactionType.INCOME)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpenses = transactions.stream()
                .filter(t -> t.getCategory().getType() == TransactionType.EXPENSE)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal currentProgress = totalIncome.subtract(totalExpenses);

        Double progressPercentage;
        if (currentProgress.compareTo(BigDecimal.ZERO) <= 0) {
            progressPercentage = 0.0;
        } else {
            progressPercentage = currentProgress
                    .divide(goal.getTargetAmount(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        BigDecimal remainingAmount = goal.getTargetAmount().subtract(currentProgress);
        if (remainingAmount.compareTo(BigDecimal.ZERO) < 0) {
            remainingAmount = BigDecimal.ZERO;
        }

        return GoalResponse.builder()
                .id(goal.getId())
                .goalName(goal.getGoalName())
                .targetAmount(goal.getTargetAmount())
                .targetDate(goal.getTargetDate())
                .startDate(goal.getStartDate())
                .currentProgress(currentProgress)
                .progressPercentage(progressPercentage)
                .remainingAmount(remainingAmount)
                .build();
    }
}
