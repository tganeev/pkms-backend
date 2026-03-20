package com.pkms.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "books")
@Data
@NoArgsConstructor
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String author;

    @Column(nullable = false)
    private String status; // In plan, In process, Stopped, Completed, Loading

    @Column(name = "total_pages")
    private Integer totalPages;

    private String language;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "total_pages_read")
    private Integer totalPagesRead = 0;

    @Column(name = "total_hours_read")
    private Double totalHoursRead = 0.0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL)
    private List<ReadingStat> readingStats;

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