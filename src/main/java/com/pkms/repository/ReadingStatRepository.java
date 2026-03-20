package com.pkms.repository;

import com.pkms.model.ReadingStat;
import com.pkms.model.ReadingStatId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReadingStatRepository extends JpaRepository<ReadingStat, ReadingStatId> {
    List<ReadingStat> findByBookId(Long bookId);
    List<ReadingStat> findByDateBetween(LocalDate startDate, LocalDate endDate);

    @Query("SELECT rs FROM ReadingStat rs WHERE rs.book.id IN :bookIds AND rs.date BETWEEN :startDate AND :endDate")
    List<ReadingStat> findByBooksAndDateRange(
            @Param("bookIds") List<Long> bookIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}