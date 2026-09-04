package com.syfe.financemanager.controller;

import com.syfe.financemanager.dto.auth.MessageResponse;
import com.syfe.financemanager.dto.goal.GoalListResponse;
import com.syfe.financemanager.dto.goal.GoalRequest;
import com.syfe.financemanager.dto.goal.GoalResponse;
import com.syfe.financemanager.dto.goal.GoalUpdateRequest;
import com.syfe.financemanager.entity.User;
import com.syfe.financemanager.service.AuthService;
import com.syfe.financemanager.service.SavingsGoalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/goals")
@Tag(name = "Savings Goals", description = "Savings goals tracking and dynamic progress calculations")
public class SavingsGoalController {

    private final SavingsGoalService goalService;
    private final AuthService authService;

    public SavingsGoalController(SavingsGoalService goalService, AuthService authService) {
        this.goalService = goalService;
        this.authService = authService;
    }

    @PostMapping
    @Operation(summary = "Create a savings goal")
    public ResponseEntity<GoalResponse> createGoal(@Valid @RequestBody GoalRequest request) {
        User currentUser = authService.getCurrentAuthenticatedUser();
        GoalResponse response = goalService.createGoal(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get all savings goals with calculated progress")
    public ResponseEntity<GoalListResponse> getAllGoals() {
        User currentUser = authService.getCurrentAuthenticatedUser();
        GoalListResponse response = goalService.getAllGoals(currentUser);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get savings goal by ID with calculated progress")
    public ResponseEntity<GoalResponse> getGoalById(@PathVariable Long id) {
        User currentUser = authService.getCurrentAuthenticatedUser();
        GoalResponse response = goalService.getGoalById(id, currentUser);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update savings goal target amount or target date")
    public ResponseEntity<GoalResponse> updateGoal(
            @PathVariable Long id,
            @Valid @RequestBody GoalUpdateRequest request) {
        User currentUser = authService.getCurrentAuthenticatedUser();
        GoalResponse response = goalService.updateGoal(id, request, currentUser);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete savings goal")
    public ResponseEntity<MessageResponse> deleteGoal(@PathVariable Long id) {
        User currentUser = authService.getCurrentAuthenticatedUser();
        MessageResponse response = goalService.deleteGoal(id, currentUser);
        return ResponseEntity.ok(response);
    }
}
