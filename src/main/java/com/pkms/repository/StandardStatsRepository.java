package com.pkms.repository;

import com.pkms.model.StandardStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StandardStatsRepository extends JpaRepository<StandardStats, Long> {

    Optional<StandardStats> findByStandardIdAndExecutionDate(Long standardId, LocalDate date);

    List<StandardStats> findByStandardIdOrderByExecutionDateAsc(Long standardId);

    @Query("SELECT COUNT(DISTINCT ss.executionDate) FROM StandardStats ss WHERE ss.standard.id = :standardId")
    Long countTotalDaysByStandardId(@Param("standardId") Long standardId);

    @Query(value = """
        WITH dates AS (
            SELECT execution_date,
                   execution_date - (ROW_NUMBER() OVER (ORDER BY execution_date))::INTEGER AS grp
            FROM standard_stats
            WHERE standard_id = :standardId
        ),
        groups AS (
            SELECT grp, COUNT(*) as group_count
            FROM dates
            GROUP BY grp
        )
        SELECT COALESCE(MAX(group_count), 0) FROM groups
        """, nativeQuery = true)
    Integer findMaxConsecutiveDays(@Param("standardId") Long standardId);

    @Modifying
    @Transactional
    @Query("DELETE FROM StandardStats ss WHERE ss.standard.id = :standardId")
    void deleteByStandardId(@Param("standardId") Long standardId);
}