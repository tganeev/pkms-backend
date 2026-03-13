package com.pkms.dto;

import lombok.Data;

@Data
public class StandardPracticeDTO {
    private Long id;
    private Long practiceId;
    private String practiceName;
    private Integer targetValue;
    private String unitType;
    private Boolean isActive;
}