package com.pkms.repository;

import com.pkms.model.PracticeLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PracticeLinkRepository extends JpaRepository<PracticeLink, Long> {

    List<PracticeLink> findBySourcePracticeId(Long sourcePracticeId);

    List<PracticeLink> findByTargetPracticeId(Long targetPracticeId);

    @Query("SELECT pl.sourcePractice.id FROM PracticeLink pl WHERE pl.targetPractice.id = :targetPracticeId")
    List<Long> findSourcePracticeIdsByTargetPracticeId(@Param("targetPracticeId") Long targetPracticeId);

    boolean existsBySourcePracticeIdAndTargetPracticeId(Long sourceId, Long targetId);
}