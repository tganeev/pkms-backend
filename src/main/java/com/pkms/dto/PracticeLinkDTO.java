package com.pkms.dto;

import lombok.Data;

@Data
public class PracticeLinkDTO {
    private Long id;
    private PracticeDTO sourcePractice;
    private PracticeDTO targetPractice;
}