package com.pkms.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.*;

@Data
public class StandardStatsDTO {
    private Long standardId;
    private String standardName;
    private String categoryName;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean isActive;
    private Long totalDays;
    private Integer maxConsecutiveDays;
    private List<StandardPracticeDTO> practices;
}