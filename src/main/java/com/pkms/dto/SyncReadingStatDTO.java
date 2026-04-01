package com.pkms.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class SyncReadingStatDTO {
    private LocalDate date;
    private Integer pagesRead;      // Сколько страниц прочитано в этот день
    private Double hoursRead;       // Сколько часов читали в этот день
}