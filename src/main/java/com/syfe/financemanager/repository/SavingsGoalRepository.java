package com.syfe.financemanager.repository;

import com.syfe.financemanager.entity.SavingsGoal;
import com.syfe.financemanager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, Long> {
    List<SavingsGoal> findByUserOrderByIdAsc(User user);
    Optional<SavingsGoal> findByIdAndUser(Long id, User user);
}
