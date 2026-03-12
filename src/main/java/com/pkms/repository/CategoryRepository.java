package com.pkms.repository;

import com.pkms.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByName(String name);

    // Если в будущем понадобится связь с пользователем, раскомментируйте эти методы
    // и добавьте поле user в модель Category
    // List<Category> findByUserIdOrIsDefaultTrue(User user);
    // List<Category> findByUserId(User user);
    // List<Category> findByIsDefaultTrue();
    // boolean existsByNameAndUserId(String name, User user);
}