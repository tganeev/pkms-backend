package com.pkms.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "reading_stats")
@Data
@NoArgsConstructor
@IdClass(ReadingStatId.class)
public class ReadingStat {

    @Id
    @ManyToOne
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Id
    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "pages_read", nullable = false)
    private Integer pagesRead = 0;

    @Column(name = "hours_read", nullable = false)
    private Double hoursRead = 0.0;

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