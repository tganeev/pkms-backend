package com.pkms.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ReadingStatDTO {
    private Long bookId;
    private LocalDate date;
    private Integer pagesRead;
    private Double hoursRead;
}