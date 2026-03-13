package com.pkms.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class DynamicTableService {

    private static final Logger log = LoggerFactory.getLogger(DynamicTableService.class);
    private final JdbcTemplate jdbcTemplate;

    public DynamicTableService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Создает таблицу для категории, если она не существует
     */
    @Transactional
    public void createCategoryTable(String categoryName) {
        String tableName = getTableName(categoryName);
        log.info("Creating table for category: {} -> table: {}", categoryName, tableName);

        String checkTableQuery = "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = ?)";
        Boolean tableExists = jdbcTemplate.queryForObject(checkTableQuery, Boolean.class, tableName);

        if (!tableExists) {
            String createTableQuery = String.format(
                    "CREATE TABLE %s (id SERIAL PRIMARY KEY, entry_date DATE NOT NULL UNIQUE, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)",
                    tableName
            );

            jdbcTemplate.execute(createTableQuery);
            log.info("✅ Created table: {}", tableName);

            String createIndexQuery = String.format("CREATE INDEX idx_%s_date ON %s(entry_date)", tableName, tableName);
            jdbcTemplate.execute(createIndexQuery);
        } else {
            log.info("Table {} already exists", tableName);
        }
    }

    /**
     * Добавляет колонку для практики в таблицу категории
     */
    @Transactional
    public void addPracticeColumn(String categoryName, String practiceName, String unitType) {
        if (practiceName == null || practiceName.trim().isEmpty()) {
            log.error("Practice name is null or empty");
            throw new RuntimeException("Practice name cannot be empty");
        }

        String tableName = getTableName(categoryName);
        String columnName = getColumnName(practiceName);
        String columnType = getColumnType(unitType);

        log.info("Adding column to table {}: {} ({})", tableName, columnName, columnType);

        // Проверяем, существует ли таблица
        String checkTableQuery = "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = ?)";
        Boolean tableExists = jdbcTemplate.queryForObject(checkTableQuery, Boolean.class, tableName);

        if (!tableExists) {
            log.error("Table {} does not exist. Create category first.", tableName);
            throw new RuntimeException("Table " + tableName + " does not exist. Create category first.");
        }

        // Проверяем, существует ли колонка
        String checkColumnQuery = "SELECT EXISTS (SELECT FROM information_schema.columns WHERE table_name = ? AND column_name = ?)";
        Boolean columnExists = jdbcTemplate.queryForObject(checkColumnQuery, Boolean.class, tableName, columnName);

        if (!columnExists) {
            String addColumnQuery = String.format("ALTER TABLE %s ADD COLUMN %s %s", tableName, columnName, columnType);
            log.info("Executing SQL: {}", addColumnQuery);
            jdbcTemplate.execute(addColumnQuery);
            log.info("✅ Added column {} to table {}", columnName, tableName);
        } else {
            log.info("Column {} already exists in table {}", columnName, tableName);
        }
    }

    /**
     * Обновляет название колонки при изменении практики
     */
    @Transactional
    public void renamePracticeColumn(String categoryName, String oldPracticeName, String newPracticeName) {
        if (oldPracticeName == null || oldPracticeName.trim().isEmpty() ||
                newPracticeName == null || newPracticeName.trim().isEmpty()) {
            log.error("Practice names are null or empty");
            throw new RuntimeException("Practice names cannot be empty");
        }

        String tableName = getTableName(categoryName);
        String oldColumnName = getColumnName(oldPracticeName);
        String newColumnName = getColumnName(newPracticeName);

        String renameColumnQuery = String.format("ALTER TABLE %s RENAME COLUMN %s TO %s",
                tableName, oldColumnName, newColumnName);

        try {
            jdbcTemplate.execute(renameColumnQuery);
            log.info("✅ Renamed column {} to {} in table {}", oldColumnName, newColumnName, tableName);
        } catch (Exception e) {
            log.error("❌ Error renaming column: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Удаляет колонку при удалении практики
     */
    @Transactional
    public void dropPracticeColumn(String categoryName, String practiceName) {
        if (practiceName == null || practiceName.trim().isEmpty()) {
            log.error("Practice name is null or empty");
            throw new RuntimeException("Practice name cannot be empty");
        }

        String tableName = getTableName(categoryName);
        String columnName = getColumnName(practiceName);

        String dropColumnQuery = String.format("ALTER TABLE %s DROP COLUMN IF EXISTS %s", tableName, columnName);

        try {
            jdbcTemplate.execute(dropColumnQuery);
            log.info("✅ Dropped column {} from table {}", columnName, tableName);
        } catch (Exception e) {
            log.error("❌ Error dropping column: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Удаляет таблицу категории
     */
    @Transactional
    public void dropCategoryTable(String categoryName) {
        String tableName = getTableName(categoryName);

        String checkTableQuery = "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = ?)";
        Boolean tableExists = jdbcTemplate.queryForObject(checkTableQuery, Boolean.class, tableName);

        if (tableExists) {
            String dropTableQuery = String.format("DROP TABLE %s CASCADE", tableName);
            jdbcTemplate.execute(dropTableQuery);
            log.info("✅ Dropped table: {}", tableName);
        }
    }

    /**
     * Сохраняет значение практики для определенной даты
     */
    @Transactional
    public void savePracticeValue(String categoryName, String practiceName, String entryDateStr, String value) {
        if (practiceName == null || practiceName.trim().isEmpty()) {
            log.error("Practice name is null or empty");
            throw new RuntimeException("Practice name cannot be empty");
        }

        String tableName = getTableName(categoryName);
        String columnName = getColumnName(practiceName);

        log.info("Saving practice value: table={}, column={}, date={}, value={}",
                tableName, columnName, entryDateStr, value);

        try {
            LocalDate entryDate;
            try {
                entryDate = LocalDate.parse(entryDateStr);
            } catch (Exception e) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                entryDate = LocalDate.parse(entryDateStr, formatter);
            }

            // Проверяем, существует ли таблица
            String checkTableQuery = "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = ?)";
            Boolean tableExists = jdbcTemplate.queryForObject(checkTableQuery, Boolean.class, tableName);

            if (!tableExists) {
                log.warn("Table {} does not exist, creating...", tableName);
                createCategoryTable(categoryName);
            }

            // Проверяем, существует ли колонка
            String checkColumnQuery = "SELECT EXISTS (SELECT FROM information_schema.columns WHERE table_name = ? AND column_name = ?)";
            Boolean columnExists = jdbcTemplate.queryForObject(checkColumnQuery, Boolean.class, tableName, columnName);

            if (!columnExists) {
                log.warn("Column {} does not exist in table {}, creating...", columnName, tableName);
                String columnType = value.matches("\\d+") ? "INTEGER" : "VARCHAR(255)";
                String addColumnQuery = String.format("ALTER TABLE %s ADD COLUMN %s %s", tableName, columnName, columnType);
                jdbcTemplate.execute(addColumnQuery);
                log.info("Column created: {} ({})", columnName, columnType);
            }

            Object parsedValue = parseValue(value);

            String checkQuery = String.format("SELECT COUNT(*) FROM %s WHERE entry_date = ?", tableName);
            Integer count = jdbcTemplate.queryForObject(checkQuery, Integer.class, entryDate);

            if (count != null && count > 0) {
                String updateQuery = String.format(
                        "UPDATE %s SET %s = ?, updated_at = CURRENT_TIMESTAMP WHERE entry_date = ?",
                        tableName, columnName
                );
                int updated = jdbcTemplate.update(updateQuery, parsedValue, entryDate);
                log.info("Updated {} rows in table {}", updated, tableName);
            } else {
                String insertQuery = String.format(
                        "INSERT INTO %s (entry_date, %s, created_at, updated_at) VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                        tableName, columnName
                );
                int inserted = jdbcTemplate.update(insertQuery, entryDate, parsedValue);
                log.info("Inserted {} rows into table {}", inserted, tableName);
            }

        } catch (Exception e) {
            log.error("Error saving practice value: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Парсит значение из строки в соответствующий тип
     */
    private Object parseValue(String value) {
        if (value == null || value.equals("-") || value.isEmpty()) {
            return null;
        }

        String numericStr = value.replaceAll("[^0-9]", "");
        if (!numericStr.isEmpty()) {
            try {
                return Integer.parseInt(numericStr);
            } catch (NumberFormatException e) {
                log.debug("Value is not a number: {}", value);
            }
        }

        if (value.matches("\\d{2}:\\d{2}")) {
            return value;
        }

        return value;
    }

    /**
     * Получает имя таблицы из названия категории
     */
    private String getTableName(String categoryName) {
        return categoryName.toLowerCase().replaceAll("[^a-z0-9]", "_") + "_practices";
    }

    /**
     * Получает имя колонки из названия практики
     */
    private String getColumnName(String practiceName) {
        return practiceName.toLowerCase()
                .replaceAll(" ", "_")
                .replaceAll("[^a-z0-9_]", "");
    }

    /**
     * Определяет тип колонки SQL на основе unitType
     */
    private String getColumnType(String unitType) {
        switch (unitType) {
            case "minutes":
                return "INTEGER";
            case "times":
                return "INTEGER";
            case "time":
                return "TIME";
            default:
                return "VARCHAR(255)";
        }
    }
}