package com.pkms.dto;

import lombok.Data;
import java.util.List;

@Data
public class CategoryDTO {
    private Long id;
    private String name;
    private String description;
    private String color;
    private String icon;
    private List<PracticeDTO> practices;
}