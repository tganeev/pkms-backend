package com.pkms.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
public class LibraryViewDTO {
    private List<BookDTO> books;                    // Левая область - список книг
    private List<LocalDate> dates;                  // Заголовки дат
    private Map<Long, Map<LocalDate, ReadingStatDTO>> stats; // Правая область - статистика по книгам и датам
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer totalPagesAll;
    private Double totalHoursAll;
}