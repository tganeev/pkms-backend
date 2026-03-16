package com.pkms.repository;

import com.pkms.model.PracticeValueLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PracticeValueLogRepository extends JpaRepository<PracticeValueLog, Long> {
    List<PracticeValueLog> findBySourceEntryId(Long sourceEntryId);
    List<PracticeValueLog> findByTargetPracticeId(Long targetPracticeId);
    void deleteBySourceEntryId(Long sourceEntryId);
}