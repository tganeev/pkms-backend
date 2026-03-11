package com.pkms.model.converter;

import com.pkms.model.enums.RepeatInterval;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RepeatIntervalConverter implements AttributeConverter<RepeatInterval, String> {

    @Override
    public String convertToDatabaseColumn(RepeatInterval attribute) {
        if (attribute == null) return null;
        return attribute.getDisplayName();
    }

    @Override
    public RepeatInterval convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;

        // Прямое сопоставление русских названий с enum
        switch (dbData) {
            case "Не повторять":
                return RepeatInterval.NONE;
            case "Каждый день":
                return RepeatInterval.DAILY;
            case "Каждую неделю":
                return RepeatInterval.WEEKLY;
            case "Каждый месяц":
                return RepeatInterval.MONTHLY;
            case "Каждый год":
                return RepeatInterval.YEARLY;
            default:
                throw new IllegalArgumentException("Unknown repeat interval: " + dbData);
        }
    }
}