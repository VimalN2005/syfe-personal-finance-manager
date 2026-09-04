package com.syfe.financemanager.service;

import com.syfe.financemanager.dto.auth.MessageResponse;
import com.syfe.financemanager.dto.goal.GoalListResponse;
import com.syfe.financemanager.dto.goal.GoalRequest;
import com.syfe.financemanager.dto.goal.GoalResponse;
import com.syfe.financemanager.dto.goal.GoalUpdateRequest;
import com.syfe.financemanager.entity.User;

public interface SavingsGoalService {
    GoalResponse createGoal(GoalRequest request, User user);
    GoalListResponse getAllGoals(User user);
    GoalResponse getGoalById(Long id, User user);
    GoalResponse updateGoal(Long id, GoalUpdateRequest request, User user);
    MessageResponse deleteGoal(Long id, User user);
}
