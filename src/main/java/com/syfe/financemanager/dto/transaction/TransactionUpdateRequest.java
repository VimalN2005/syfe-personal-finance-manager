package com.syfe.financemanager.dto.transaction;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
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
public class TransactionUpdateRequest {

    @DecimalMin(value = "0.01", message = "Amount must be a positive decimal value")
    private BigDecimal amount;

    private String category;

    private String description;

    /**
     * Date field may be sent in request bodies, but according to business rules,
     * the date of an existing transaction is immutable and will not be changed.
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate date;
}
