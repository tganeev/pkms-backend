package com.pkms.repository;

import com.pkms.model.StandardPractice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface StandardPracticeRepository extends JpaRepository<StandardPractice, Long> {
    List<StandardPractice> findByStandardId(Long standardId);
    void deleteByStandardId(Long standardId);
}