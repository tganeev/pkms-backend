package com.pkms.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "practices")
@Data
@NoArgsConstructor
public class Practice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    @JsonIgnore // Игнорируем поле category при сериализации в JSON
    private Category category;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "unit_type")
    private String unitType;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}