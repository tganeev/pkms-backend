package com.pkms.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
public class CategoryTableDTO {
    private String categoryName;
    private List<String> practices; // Названия практик по горизонтали
    private List<RowData> rows; // Строки с данными по датам

    @Data
    public static class RowData {
        private LocalDate date;
        private Map<String, String> values; // Практика -> значение (мин/раз/время)
        //private String standard; // Стандарт на эту дату (Ядро/Стандарт 1/и т.д.)
    }
}