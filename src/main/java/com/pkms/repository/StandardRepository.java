package com.pkms.repository;

import com.pkms.model.Standard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface StandardRepository extends JpaRepository<Standard, Long> {
    List<Standard> findByCategoryId(Long categoryId);
    List<Standard> findByCategoryIdOrderByCreatedAtDesc(Long categoryId);
}