package com.syfe.financemanager.dto.category;

import com.syfe.financemanager.entity.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRequest {

    @NotBlank(message = "Category name cannot be blank")
    private String name;

    @NotNull(message = "Category type is required (INCOME or EXPENSE)")
    private TransactionType type;
}
