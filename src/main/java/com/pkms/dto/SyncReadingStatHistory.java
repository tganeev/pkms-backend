package com.pkms.dto;

import lombok.Data;

@Data
public class SyncReadingStatHistory {
    private String bookServerId;
    private String date;
    private Integer pagesRead;
    private Double hoursRead;
}