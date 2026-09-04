package com.syfe.financemanager.service;

import com.syfe.financemanager.dto.report.MonthlyReportResponse;
import com.syfe.financemanager.dto.report.YearlyReportResponse;
import com.syfe.financemanager.entity.User;

public interface ReportService {
    MonthlyReportResponse getMonthlyReport(int year, int month, User user);
    YearlyReportResponse getYearlyReport(int year, User user);
}
