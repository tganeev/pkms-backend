package com.pkms.repository;

import com.pkms.model.StandardPractice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Repository
public interface StandardPracticeRepository extends JpaRepository<StandardPractice, Long> {
    List<StandardPractice> findByStandardId(Long standardId);

    @Modifying
    @Transactional
    @Query("DELETE FROM StandardPractice sp WHERE sp.standard.id = :standardId")
    void deleteByStandardId(@Param("standardId") Long standardId);
}