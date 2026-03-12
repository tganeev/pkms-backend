package com.pkms.service;

import com.pkms.dto.CategoryTableDTO;
import com.pkms.model.*;
import com.pkms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryTableService {

    private final CategoryRepository categoryRepository;
    private final PracticeRepository practiceRepository;
    private final CalendarRepository calendarRepository;
    private final YogaPracticeRepository yogaPracticeRepository;
    private final PracticeStandardRepository practiceStandardRepository;

    public CategoryTableDTO getCategoryTable(String categoryName, LocalDate startDate, LocalDate endDate) {
        Category category = categoryRepository.findByName(categoryName)
                .orElseThrow(() -> new RuntimeException("Category not found: " + categoryName));

        CategoryTableDTO dto = new CategoryTableDTO();
        dto.setCategoryName(categoryName);

        // Получаем все практики для этой категории
        List<Practice> practices = practiceRepository.findByCategoryId(category.getId());
        List<String> practiceNames = practices.stream()
                .map(Practice::getName)
                .collect(Collectors.toList());
        dto.setPractices(practiceNames);

        // Получаем данные за указанный период
        List<CategoryTableDTO.RowData> rows = new ArrayList<>();

        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            CategoryTableDTO.RowData row = new CategoryTableDTO.RowData();
            row.setDate(currentDate);

            // Получаем значения для каждой практики
            Map<String, String> values = new HashMap<>();

            // В зависимости от категории, получаем данные из соответствующей таблицы
            switch (categoryName) {
                case "Yoga":
                    fillYogaData(currentDate, values, practiceNames);
                    break;
                // Добавить другие категории по мере создания таблиц
                default:
                    fillGenericData(currentDate, values, categoryName, practiceNames);
            }

            row.setValues(values);

            // Определяем стандарт на эту дату (можно брать из отдельной таблицы или вычислять)
            String standard = determineStandard(currentDate, categoryName, values);
            //row.setStandard(standard);

            rows.add(row);
            currentDate = currentDate.plusDays(1);
        }

        dto.setRows(rows);
        return dto;
    }

    private void fillYogaData(LocalDate date, Map<String, String> values, List<String> practiceNames) {
        Optional<YogaPractice> yogaPractice = yogaPracticeRepository.findByEntryDate(date);

        if (yogaPractice.isPresent()) {
            YogaPractice yp = yogaPractice.get();

            for (String practiceName : practiceNames) {
                switch (practiceName) {
                    case "Концентрация":
                        values.put(practiceName, yp.getConcentrationMin() != null ?
                                yp.getConcentrationMin() + " мин" : "-");
                        break;
                    case "Аналитическая медитация":
                        values.put(practiceName, yp.getAnalyticalMeditationMin() != null ?
                                yp.getAnalyticalMeditationMin() + " мин" : "-");
                        break;
                    case "Пранаяма":
                        values.put(practiceName, yp.getPranayamaMin() != null ?
                                yp.getPranayamaMin() + " мин" : "-");
                        break;
                    case "Экадаш":
                        values.put(practiceName, yp.getEkadashCount() != null ?
                                yp.getEkadashCount() + " раз" : "-");
                        break;
                    case "Подъем":
                        values.put(practiceName, yp.getWakeUpTime() != null ?
                                yp.getWakeUpTime().toString() : "-");
                        break;
                    case "Отбой":
                        values.put(practiceName, yp.getSleepTime() != null ?
                                yp.getSleepTime().toString() : "-");
                        break;
                    default:
                        values.put(practiceName, "-");
                }
            }
        } else {
            // Нет данных на эту дату
            for (String practiceName : practiceNames) {
                values.put(practiceName, "-");
            }
        }
    }

    private void fillGenericData(LocalDate date, Map<String, String> values,
                                 String categoryName, List<String> practiceNames) {
        // Для других категорий получаем данные из calendar
        List<CalendarEntry> entries = calendarRepository.findByUserAndEntryDate(null, date);
        // TODO: добавить пользователя

        for (String practiceName : practiceNames) {
            Optional<CalendarEntry> entry = entries.stream()
                    .filter(e -> e.getCategory().equals(categoryName) && e.getPractice().equals(practiceName))
                    .findFirst();

            if (entry.isPresent()) {
                values.put(practiceName, entry.get().getDuration());
            } else {
                values.put(practiceName, "-");
            }
        }
    }

    private String determineStandard(LocalDate date, String categoryName, Map<String, String> values) {
        // Здесь логика определения стандарта на основе выполненных практик
        // Можно брать из отдельной таблицы или вычислять по пороговым значениям

        // Пока возвращаем заглушку
        if (values.values().stream().anyMatch(v -> !v.equals("-"))) {
            return "Стандарт 1"; // Временное решение
        }
        return "-";
    }
}