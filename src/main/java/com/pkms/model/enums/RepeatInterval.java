package com.pkms.model.enums;

public enum RepeatInterval {
    NONE("Не повторять"),
    DAILY("Каждый день"),
    WEEKLY("Каждую неделю"),
    MONTHLY("Каждый месяц"),
    YEARLY("Каждый год");

    private final String displayName;

    RepeatInterval(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Получить enum по русскому названию
     * @param displayName русское название интервала повторения
     * @return соответствующий enum или NONE если не найдено
     */
    public static RepeatInterval fromDisplayName(String displayName) {
        if (displayName == null || displayName.isEmpty()) {
            return NONE;
        }

        for (RepeatInterval interval : RepeatInterval.values()) {
            if (interval.displayName.equals(displayName)) {
                return interval;
            }
        }

        // Если не нашли соответствия, выбрасываем исключение
        throw new IllegalArgumentException("Unknown repeat interval: " + displayName);
    }

    /**
     * Получить enum по русскому названию с значением по умолчанию
     * @param displayName русское название интервала повторения
     * @param defaultValue значение по умолчанию
     * @return соответствующий enum или defaultValue если не найдено
     */
    public static RepeatInterval fromDisplayNameOrDefault(String displayName, RepeatInterval defaultValue) {
        if (displayName == null || displayName.isEmpty()) {
            return defaultValue;
        }

        for (RepeatInterval interval : RepeatInterval.values()) {
            if (interval.displayName.equals(displayName)) {
                return interval;
            }
        }

        return defaultValue;
    }

    @Override
    public String toString() {
        return displayName;
    }
}