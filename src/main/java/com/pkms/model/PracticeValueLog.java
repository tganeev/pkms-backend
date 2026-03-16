package com.pkms.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "practice_value_logs")
@Data
@NoArgsConstructor
public class PracticeValueLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_entry_id", nullable = false)
    private Long sourceEntryId;

    @ManyToOne
    @JoinColumn(name = "source_practice_id", nullable = false)
    private Practice sourcePractice;

    @ManyToOne
    @JoinColumn(name = "target_practice_id", nullable = false)
    private Practice targetPractice;

    @Column(nullable = false)
    private Integer value;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "operation_type", nullable = false)
    private String operationType; // 'ADD' или 'SUBTRACT'

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}