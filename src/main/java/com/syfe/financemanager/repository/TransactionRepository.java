package com.syfe.financemanager.repository;

import com.syfe.financemanager.entity.Category;
import com.syfe.financemanager.entity.Transaction;
import com.syfe.financemanager.entity.TransactionType;
import com.syfe.financemanager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    List<Transaction> findByUserOrderByDateDescIdDesc(User user);

    Optional<Transaction> findByIdAndUser(Long id, User user);

    boolean existsByCategoryAndUser(Category category, User user);

    boolean existsByCategory(Category category);

    @Query("SELECT t FROM Transaction t WHERE t.user = :user AND t.date >= :startDate")
    List<Transaction> findByUserAndDateGreaterThanEqual(@Param("user") User user, @Param("startDate") LocalDate startDate);

    @Query("SELECT t FROM Transaction t WHERE t.user = :user AND t.date >= :startDate AND t.date <= :endDate")
    List<Transaction> findByUserAndDateBetween(@Param("user") User user, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
