package com.pkms.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class EntryDTO {
    private Long id;
    private String category;
    private String practice;
    private String duration;
    private String period;
    private String repeatInterval;
    private LocalDate entryDate;
}