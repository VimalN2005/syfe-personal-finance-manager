package com.syfe.financemanager.config;

import com.syfe.financemanager.entity.Category;
import com.syfe.financemanager.entity.TransactionType;
import com.syfe.financemanager.repository.CategoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Initializes predefined default categories upon system startup.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    public DataInitializer(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public void run(String... args) {
        seedDefaultCategories();
    }

    private void seedDefaultCategories() {
        List<DefaultCategoryDef> defaults = List.of(
                new DefaultCategoryDef("Salary", TransactionType.INCOME),
                new DefaultCategoryDef("Food", TransactionType.EXPENSE),
                new DefaultCategoryDef("Rent", TransactionType.EXPENSE),
                new DefaultCategoryDef("Transportation", TransactionType.EXPENSE),
                new DefaultCategoryDef("Entertainment", TransactionType.EXPENSE),
                new DefaultCategoryDef("Healthcare", TransactionType.EXPENSE),
                new DefaultCategoryDef("Utilities", TransactionType.EXPENSE)
        );

        for (DefaultCategoryDef def : defaults) {
            if (categoryRepository.findByNameIgnoreCaseAndUserIsNull(def.name()).isEmpty()) {
                Category category = Category.builder()
                        .name(def.name())
                        .type(def.type())
                        .isCustom(false)
                        .user(null)
                        .build();
                categoryRepository.save(category);
            }
        }
    }

    private record DefaultCategoryDef(String name, TransactionType type) {}
}
