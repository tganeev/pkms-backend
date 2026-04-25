package com.pkms.dto;

import lombok.Data;

@Data
public class SyncHistoryRequest {
    private String username;
    private Long lastSyncTimestamp;
}