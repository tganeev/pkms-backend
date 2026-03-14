package com.pkms.repository;

import com.pkms.model.Standard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Repository
public interface StandardRepository extends JpaRepository<Standard, Long> {
    List<Standard> findByCategoryId(Long categoryId);
    List<Standard> findByCategoryIdOrderByStartDateDesc(Long categoryId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Standard s WHERE s.category.id = :categoryId")
    void deleteByCategoryId(@Param("categoryId") Long categoryId);
}