package com.pkms.model;

import com.pkms.model.enums.Period;
import com.pkms.model.enums.RepeatInterval;
import com.pkms.model.converter.RepeatIntervalConverter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "calendar")
@Data
@NoArgsConstructor
public class CalendarEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String practice;

    @Column(nullable = false)
    private String duration;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Period period;

    @Convert(converter = RepeatIntervalConverter.class)
    @Column(name = "repeat_interval")
    private RepeatInterval repeatInterval;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "status")
    private String status; // 'completed', 'partial', 'failed'

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "calendarEntry", cascade = CascadeType.ALL)
    @JsonIgnore
    private EntryStatus entryStatus;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Методы для удобства работы со статусом
    public boolean isCompleted() {
        return "completed".equals(status);
    }

    public boolean isPartial() {
        return "partial".equals(status);
    }

    public boolean isFailed() {
        return "failed".equals(status);
    }

    public boolean hasStatus() {
        return status != null && !status.isEmpty();
    }

    @Override
    public String toString() {
        return "CalendarEntry{" +
                "id=" + id +
                ", category='" + category + '\'' +
                ", practice='" + practice + '\'' +
                ", duration='" + duration + '\'' +
                ", period=" + period +
                ", repeatInterval=" + (repeatInterval != null ? repeatInterval.getDisplayName() : "null") +
                ", entryDate=" + entryDate +
                ", status='" + status + '\'' +
                '}';
    }
}