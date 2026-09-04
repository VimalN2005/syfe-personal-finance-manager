package com.syfe.financemanager.repository;

import com.syfe.financemanager.entity.Category;
import com.syfe.financemanager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query("SELECT c FROM Category c WHERE c.user IS NULL OR c.user = :user ORDER BY c.id ASC")
    List<Category> findAllAccessibleByUser(@Param("user") User user);

    Optional<Category> findByNameIgnoreCaseAndUserIsNull(String name);

    Optional<Category> findByNameIgnoreCaseAndUser(String name, User user);

    @Query("SELECT c FROM Category c WHERE LOWER(c.name) = LOWER(:name) AND (c.user IS NULL OR c.user = :user)")
    Optional<Category> findByNameAccessibleByUser(@Param("name") String name, @Param("user") User user);

    boolean existsByNameIgnoreCaseAndUser(String name, User user);

    boolean existsByNameIgnoreCaseAndUserIsNull(String name);
}
