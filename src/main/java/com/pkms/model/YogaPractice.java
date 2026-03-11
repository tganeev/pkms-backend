package com.pkms.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Entity
@Table(name = "yoga_practices")
@Data
@NoArgsConstructor
public class YogaPractice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entry_date", nullable = false, unique = true)
    private LocalDate entryDate;

    @Column(name = "concentration_min")
    private Integer concentrationMin;

    @Column(name = "analytical_meditation_min")
    private Integer analyticalMeditationMin;

    @Column(name = "pranayama_min")
    private Integer pranayamaMin;

    @Column(name = "ekadash_count")
    private Integer ekadashCount;

    @Column(name = "wake_up_time")
    private LocalTime wakeUpTime;

    @Column(name = "sleep_time")
    private LocalTime sleepTime;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}