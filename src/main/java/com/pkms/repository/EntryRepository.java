package com.pkms.repository;

import com.pkms.model.Entry;
import com.pkms.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface EntryRepository extends JpaRepository<Entry, Long> {
    List<Entry> findByUserAndEntryDateBetweenOrderByEntryDateAscPeriodAsc(
            User user, LocalDate startDate, LocalDate endDate);

    List<Entry> findByUserAndEntryDate(User user, LocalDate date);

    void deleteByUserIdAndId(Long userId, Long entryId);
}