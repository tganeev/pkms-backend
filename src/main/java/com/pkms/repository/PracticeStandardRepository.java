package com.pkms.repository;

import com.pkms.model.PracticeStandard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PracticeStandardRepository extends JpaRepository<PracticeStandard, Long> {
    List<PracticeStandard> findByCategoryId(Long categoryId);
    List<PracticeStandard> findByCategoryIdAndStandardName(Long categoryId, String standardName);
}