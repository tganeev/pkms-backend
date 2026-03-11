package com.pkms.model.enums;

public enum Period {
    morning("morning", "Утро", "06:00-12:00"),
    day("day", "День", "12:00-18:00"),
    evening("evening", "Вечер", "18:00-00:00");

    private final String code;
    private final String displayName;
    private final String timeRange;

    Period(String code, String displayName, String timeRange) {
        this.code = code;
        this.displayName = displayName;
        this.timeRange = timeRange;
    }

    public String getCode() { return code; }
    public String getDisplayName() { return displayName; }
    public String getTimeRange() { return timeRange; }
}