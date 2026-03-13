package com.pkms.dto;

import lombok.Data;
import java.util.List;

@Data
public class StandardDTO {
    private Long id;
    private String name;
    private String description;
    private Long categoryId;
    private String categoryName;
    private List<StandardPracticeDTO> practices;
}