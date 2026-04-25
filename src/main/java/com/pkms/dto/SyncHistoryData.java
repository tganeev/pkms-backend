package com.pkms.dto;

import lombok.Data;
import java.util.List;

@Data
public class SyncHistoryData {
    private List<SyncBookHistory> books;
    private List<SyncReadingStatHistory> readingStats;
    private long lastSyncTimestamp;
}