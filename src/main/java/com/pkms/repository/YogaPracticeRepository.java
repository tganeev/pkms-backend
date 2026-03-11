package com.pkms.repository;

import com.pkms.model.YogaPractice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface YogaPracticeRepository extends JpaRepository<YogaPractice, Long> {
    Optional<YogaPractice> findByEntryDate(LocalDate date);
}