package com.syfe.financemanager.controller;

import com.syfe.financemanager.dto.report.MonthlyReportResponse;
import com.syfe.financemanager.dto.report.YearlyReportResponse;
import com.syfe.financemanager.entity.User;
import com.syfe.financemanager.service.AuthService;
import com.syfe.financemanager.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@Tag(name = "Reports and Analytics", description = "Financial analytics, monthly breakdowns, and yearly summaries")
public class ReportController {

    private final ReportService reportService;
    private final AuthService authService;

    public ReportController(ReportService reportService, AuthService authService) {
        this.reportService = reportService;
        this.authService = authService;
    }

    @GetMapping("/monthly/{year}/{month}")
    @Operation(summary = "Generate monthly report with income/expense category breakdown and net savings")
    public ResponseEntity<MonthlyReportResponse> getMonthlyReport(@PathVariable int year, @PathVariable int month) {
        User currentUser = authService.getCurrentAuthenticatedUser();
        MonthlyReportResponse response = reportService.getMonthlyReport(year, month, currentUser);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/yearly/{year}")
    @Operation(summary = "Generate yearly report with comprehensive annual financial overview")
    public ResponseEntity<YearlyReportResponse> getYearlyReport(@PathVariable int year) {
        User currentUser = authService.getCurrentAuthenticatedUser();
        YearlyReportResponse response = reportService.getYearlyReport(year, currentUser);
        return ResponseEntity.ok(response);
    }
}
