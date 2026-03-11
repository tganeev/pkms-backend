package com.pkms.repository;

import com.pkms.model.CalendarEntry;
import com.pkms.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface CalendarRepository extends JpaRepository<CalendarEntry, Long> {
    List<CalendarEntry> findByUserAndEntryDateBetweenOrderByEntryDateAscPeriodAsc(
            User user, LocalDate startDate, LocalDate endDate);

    List<CalendarEntry> findByUserAndEntryDate(User user, LocalDate date);

    void deleteByUserIdAndId(Long userId, Long entryId);
}