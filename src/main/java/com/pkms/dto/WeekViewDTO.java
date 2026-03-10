package com.pkms.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
public class WeekViewDTO {
    private LocalDate startDate;
    private LocalDate endDate;
    private List<DayDTO> days;

    @Data
    public static class DayDTO {
        private LocalDate date;
        private String dayName;
        private List<EntryDTO> morning;
        private List<EntryDTO> day;
        private List<EntryDTO> evening;
    }
}