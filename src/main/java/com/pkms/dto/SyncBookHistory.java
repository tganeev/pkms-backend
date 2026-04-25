package com.pkms.dto;

import lombok.Data;

@Data
public class SyncBookHistory {
    private String serverId;
    private String title;
    private String author;
    private Integer totalPages;
    private Integer currentPage;
    private Long readingTime;
    private String status;
    private Long lastReadDate;
}