package com.pkms.repository;

import com.pkms.model.Practice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Repository
public interface PracticeRepository extends JpaRepository<Practice, Long> {
    List<Practice> findByCategoryId(Long categoryId);

    Optional<Practice> findByCategoryIdAndName(Long categoryId, String name);

    @Modifying
    @Transactional
    @Query("DELETE FROM Practice p WHERE p.category.id = :categoryId")
    void deleteByCategoryId(@Param("categoryId") Long categoryId);
}