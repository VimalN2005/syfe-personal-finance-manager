package com.syfe.financemanager.controller;

import com.syfe.financemanager.dto.report.MonthlyReportResponse;
import com.syfe.financemanager.dto.report.YearlyReportResponse;
import com.syfe.financemanager.entity.User;
import com.syfe.financemanager.service.AuthService;
import com.syfe.financemanager.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportService reportService;

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
    void testGetMonthlyReport() throws Exception {
        MonthlyReportResponse response = MonthlyReportResponse.builder()
                .month(1)
                .year(2024)
                .totalIncome(Map.of("Salary", new BigDecimal("5000.00")))
                .totalExpenses(Map.of("Food", new BigDecimal("500.00")))
                .netSavings(new BigDecimal("4500.00"))
                .build();

        when(reportService.getMonthlyReport(2024, 1, mockUser)).thenReturn(response);

        mockMvc.perform(get("/api/reports/monthly/2024/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value(1))
                .andExpect(jsonPath("$.year").value(2024))
                .andExpect(jsonPath("$.netSavings").value(4500.00));
    }

    @Test
    @WithMockUser
    void testGetYearlyReport() throws Exception {
        YearlyReportResponse response = YearlyReportResponse.builder()
                .year(2024)
                .totalIncome(Map.of("Salary", new BigDecimal("60000.00")))
                .totalExpenses(Map.of("Food", new BigDecimal("6000.00")))
                .netSavings(new BigDecimal("54000.00"))
                .build();

        when(reportService.getYearlyReport(2024, mockUser)).thenReturn(response);

        mockMvc.perform(get("/api/reports/yearly/2024"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2024))
                .andExpect(jsonPath("$.netSavings").value(54000.00));
    }
}
