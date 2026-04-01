package com.pkms.dto;

import lombok.Data;
import java.util.List;

@Data
public class BookDTO {
    private Long id;
    private String title;
    private String author;
    private String identifier;  // Новое поле
    private String status;
    private Integer totalPages;
    private String language;
    private Long categoryId;
    private String categoryName;
    private Integer totalPagesRead;
    private Double totalHoursRead;
    private List<ReadingStatDTO> stats;
}