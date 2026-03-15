package com.pkms.dto;

import lombok.Data;

@Data
public class PracticeDTO {
    private Long id;
    private String name;
    private String description;
    private String unitType;
    private Integer displayOrder;
    private Long categoryId;
    private String categoryName;
}