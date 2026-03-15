package com.pkms.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "practice_links")
@Data
@NoArgsConstructor
public class PracticeLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "source_practice_id", nullable = false)
    private Practice sourcePractice; // Практика-источник (откуда берутся данные)

    @ManyToOne
    @JoinColumn(name = "target_practice_id", nullable = false)
    private Practice targetPractice; // Практика-цель (куда добавляются данные)

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}