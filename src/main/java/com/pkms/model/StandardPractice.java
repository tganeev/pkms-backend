package com.pkms.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "standard_practices")
@Data
@NoArgsConstructor
public class StandardPractice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "standard_id", nullable = false)
    private Standard standard;

    @ManyToOne
    @JoinColumn(name = "practice_id", nullable = false)
    private Practice practice;

    @Column(nullable = false)
    private Integer targetValue; // целевое значение

    @Column(nullable = false)
    private String unitType; // minutes, times, time

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}