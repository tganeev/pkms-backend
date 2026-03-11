package com.pkms.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "practice_standards")
@Data
@NoArgsConstructor
public class PracticeStandard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "standard_name", nullable = false)
    private String standardName;

    @Column(name = "practice_name", nullable = false)
    private String practiceName;

    @Column(name = "target_value", nullable = false)
    private Integer targetValue;

    @Column(name = "unit_type", nullable = false)
    private String unitType;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}