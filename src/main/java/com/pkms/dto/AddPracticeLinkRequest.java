package com.pkms.dto;

import lombok.Data;

@Data
public class AddPracticeLinkRequest {
    private Long sourcePracticeId;
    private Long targetPracticeId;
}