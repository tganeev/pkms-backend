package com.pkms.dto;

import lombok.Data;
import java.util.List;

@Data
public class SyncBookDTO {
    private String identifier;      // Уникальный идентификатор книги (хэш или ISBN)
    private String title;
    private String author;
    private Integer totalPages;
    private String language;
    private Long categoryId;
    private List<SyncReadingStatDTO> readingStats; // Ежедневная статистика
}