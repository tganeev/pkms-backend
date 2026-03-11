package com.pkms.dto;

import lombok.Data;

@Data
public class EntryStatusDTO {
    private Long entryId;
    private String status; // 'completed', 'partial', 'failed'
    private String notes;
}