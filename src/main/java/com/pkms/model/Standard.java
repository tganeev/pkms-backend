package com.pkms.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "standards")
@Data
@NoArgsConstructor
public class Standard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate; // Дата ввода стандарта

    @Column(name = "end_date")
    private LocalDate endDate; // Дата вывода стандарта (null если действует)

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "standard", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StandardPractice> standardPractices;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (startDate == null) {
            startDate = LocalDate.now(); // По умолчанию - сегодня
        }
    }

    // Метод для определения статуса стандарта
    public boolean isActive() {
        LocalDate today = LocalDate.now();
        if (startDate == null) return false;
        if (today.isBefore(startDate)) return false;
        if (endDate != null && today.isAfter(endDate)) return false;
        return true;
    }
}