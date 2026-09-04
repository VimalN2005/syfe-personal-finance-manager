package com.syfe.financemanager.dto.goal;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.syfe.financemanager.common.ExactDecimalSerializer;
import com.syfe.financemanager.common.MonetaryAmountSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoalResponse {

    private Long id;
    private String goalName;

    @JsonSerialize(using = ExactDecimalSerializer.class)
    private BigDecimal targetAmount;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate targetDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonSerialize(using = MonetaryAmountSerializer.class)
    private BigDecimal currentProgress;

    private Double progressPercentage;

    @JsonSerialize(using = ExactDecimalSerializer.class)
    private BigDecimal remainingAmount;
}
