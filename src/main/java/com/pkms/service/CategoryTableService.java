package com.pkms.service;

import com.pkms.dto.CategoryTableDTO;
import com.pkms.model.*;
import com.pkms.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.dao.EmptyResultDataAccessException;

@Service
@RequiredArgsConstructor
public class CategoryTableService {

    private static final Logger log = LoggerFactory.getLogger(CategoryTableService.class);

    private final CategoryRepository categoryRepository;
    private final PracticeRepository practiceRepository;
    private final CalendarRepository calendarRepository;
    private final YogaPracticeRepository yogaPracticeRepository;
    private final PracticeStandardRepository practiceStandardRepository;
    private final JdbcTemplate jdbcTemplate;

    public CategoryTableDTO getCategoryTable(String categoryName, LocalDate startDate, LocalDate endDate) {
        log.info("=== GET CATEGORY TABLE ===");
        log.info("Category: {}, from: {}, to: {}", categoryName, startDate, endDate);

        Category category = categoryRepository.findByName(categoryName)
                .orElseThrow(() -> new RuntimeException("Category not found: " + categoryName));

        CategoryTableDTO dto = new CategoryTableDTO();
        dto.setCategoryName(categoryName);

        // Получаем все практики для этой категории
        List<Practice> practices = practiceRepository.findByCategoryId(category.getId());
        List<String> practiceNames = practices.stream()
                .map(Practice::getName)
                .collect(Collectors.toList());
        dto.setPractices(practiceNames != null ? practiceNames : new ArrayList<>());

        log.info("Practices for category {}: {}", categoryName, practiceNames);

        // Получаем данные из динамической таблицы
        String tableName = categoryName.toLowerCase() + "_practices";
        log.info("Looking for data in table: {}", tableName);

        List<CategoryTableDTO.RowData> rows = new ArrayList<>();

        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            CategoryTableDTO.RowData row = new CategoryTableDTO.RowData();
            row.setDate(currentDate);

            // Получаем значения из динамической таблицы для этой даты
            Map<String, String> values = new HashMap<>();

            // Для каждой практики получаем значение из БД
            for (String practiceName : practiceNames) {
                String columnName = practiceName.toLowerCase().replace(" ", "_");
                String value = getPracticeValueFromDB(tableName, columnName, currentDate);
                values.put(practiceName, value);
            }

            row.setValues(values);
            rows.add(row);

            currentDate = currentDate.plusDays(1);
        }

        dto.setRows(rows);
        log.info("Returning {} rows for category {}", rows.size(), categoryName);
        return dto;
    }

    private String getPracticeValueFromDB(String tableName, String columnName, LocalDate date) {
        try {
            // Проверим, существует ли таблица
            String checkTableQuery = "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = ?)";
            Boolean tableExists = jdbcTemplate.queryForObject(checkTableQuery, Boolean.class, tableName);

            if (!tableExists) {
                log.warn("Table {} does not exist", tableName);
                return "-";
            }

            // Проверим, существует ли колонка
            String checkColumnQuery = "SELECT EXISTS (SELECT FROM information_schema.columns WHERE table_name = ? AND column_name = ?)";
            Boolean columnExists = jdbcTemplate.queryForObject(checkColumnQuery, Boolean.class, tableName, columnName);

            if (!columnExists) {
                log.warn("Column {} does not exist in table {}", columnName, tableName);
                return "-";
            }

            // Получаем данные
            String query = String.format("SELECT %s FROM %s WHERE entry_date = ?", columnName, tableName);
            log.debug("Executing query: {} with date: {}", query, date);

            try {
                Object result = jdbcTemplate.queryForObject(query, Object.class, date);
                log.info("Query result for date {}: {}", date, result);
                return result != null ? result.toString() : "-";
            } catch (EmptyResultDataAccessException e) {
                log.info("No data for date {} in table {}", date, tableName);
                return "-";
            }
        } catch (Exception e) {
            log.error("Error getting value for date {} from table {}: {}", date, tableName, e.getMessage());
            return "-";
        }
    }
}