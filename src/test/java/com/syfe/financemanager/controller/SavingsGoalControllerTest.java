package com.syfe.financemanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syfe.financemanager.dto.auth.MessageResponse;
import com.syfe.financemanager.dto.goal.GoalListResponse;
import com.syfe.financemanager.dto.goal.GoalRequest;
import com.syfe.financemanager.dto.goal.GoalResponse;
import com.syfe.financemanager.dto.goal.GoalUpdateRequest;
import com.syfe.financemanager.entity.User;
import com.syfe.financemanager.service.AuthService;
import com.syfe.financemanager.service.SavingsGoalService;
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
class SavingsGoalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SavingsGoalService goalService;

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
    void testCreateGoal() throws Exception {
        GoalRequest request = GoalRequest.builder()
                .goalName("Emergency Fund")
                .targetAmount(new BigDecimal("10000.00"))
                .targetDate(LocalDate.now().plusYears(1))
                .startDate(LocalDate.now())
                .build();

        GoalResponse response = GoalResponse.builder()
                .id(1L)
                .goalName("Emergency Fund")
                .targetAmount(new BigDecimal("10000.00"))
                .targetDate(LocalDate.now().plusYears(1))
                .startDate(LocalDate.now())
                .currentProgress(new BigDecimal("2000.00"))
                .progressPercentage(20.0)
                .remainingAmount(new BigDecimal("8000.00"))
                .build();

        when(goalService.createGoal(any(), eq(mockUser))).thenReturn(response);

        mockMvc.perform(post("/api/goals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.goalName").value("Emergency Fund"))
                .andExpect(jsonPath("$.progressPercentage").value(20.0));
    }

    @Test
    @WithMockUser
    void testGetAllGoals() throws Exception {
        GoalResponse response = GoalResponse.builder().id(1L).goalName("Emergency Fund").build();
        when(goalService.getAllGoals(mockUser))
                .thenReturn(GoalListResponse.builder().goals(List.of(response)).build());

        mockMvc.perform(get("/api/goals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.goals[0].id").value(1));
    }

    @Test
    @WithMockUser
    void testGetGoalById() throws Exception {
        GoalResponse response = GoalResponse.builder().id(1L).goalName("Emergency Fund").build();
        when(goalService.getGoalById(1L, mockUser)).thenReturn(response);

        mockMvc.perform(get("/api/goals/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser
    void testUpdateGoal() throws Exception {
        GoalUpdateRequest request = GoalUpdateRequest.builder().targetAmount(new BigDecimal("12000.00")).build();
        GoalResponse response = GoalResponse.builder().id(1L).targetAmount(new BigDecimal("12000.00")).build();

        when(goalService.updateGoal(eq(1L), any(), eq(mockUser))).thenReturn(response);

        mockMvc.perform(put("/api/goals/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetAmount").value(12000.00));
    }

    @Test
    @WithMockUser
    void testDeleteGoal() throws Exception {
        when(goalService.deleteGoal(1L, mockUser))
                .thenReturn(new MessageResponse("Goal deleted successfully"));

        mockMvc.perform(delete("/api/goals/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Goal deleted successfully"));
    }
}
