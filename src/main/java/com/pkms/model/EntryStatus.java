package com.pkms.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "entry_status")
@Data
@NoArgsConstructor
public class EntryStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "calendar_entry_id", nullable = false)
    private CalendarEntry calendarEntry;

    @Column(nullable = false)
    private String status; // 'completed', 'partial', 'failed'

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}