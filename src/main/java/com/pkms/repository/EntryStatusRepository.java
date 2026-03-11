package com.pkms.repository;

import com.pkms.model.EntryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface EntryStatusRepository extends JpaRepository<EntryStatus, Long> {
    Optional<EntryStatus> findByCalendarEntryId(Long calendarEntryId);
}