package com.pkms.dto;

import lombok.Data;

@Data
public class SyncHistoryResponse {
    private boolean success;
    private SyncHistoryData data;
    private String error;
}