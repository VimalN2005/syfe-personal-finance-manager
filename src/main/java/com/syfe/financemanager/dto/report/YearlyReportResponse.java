package com.syfe.financemanager.dto.report;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.syfe.financemanager.common.MonetaryAmountSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YearlyReportResponse {

    private int year;
    private Map<String, BigDecimal> totalIncome;
    private Map<String, BigDecimal> totalExpenses;

    @JsonSerialize(using = MonetaryAmountSerializer.class)
    private BigDecimal netSavings;
}
