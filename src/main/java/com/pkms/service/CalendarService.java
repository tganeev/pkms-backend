package com.pkms.service;

import com.pkms.dto.EntryDTO;
import com.pkms.dto.EntryStatusDTO;
import com.pkms.model.*;
import com.pkms.model.enums.Period;
import com.pkms.model.enums.RepeatInterval;
import com.pkms.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Map; // ДОБАВЛЯЕМ ЭТОТ ИМПОРТ
import java.util.HashMap; // ДОБАВЛЯЕМ ЭТОТ ИМПОРТ
import java.util.stream.Collectors;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CalendarService {

    private static final Logger log = LoggerFactory.getLogger(CalendarService.class);

    private final CalendarRepository calendarRepository;
    private final UserRepository userRepository;
    private final EntryStatusRepository entryStatusRepository;
    private final PracticeStandardRepository practiceStandardRepository;
    private final YogaPracticeRepository yogaPracticeRepository;
    private final DynamicTableService dynamicTableService;
    private final CategoryRepository categoryRepository;
    private final JdbcTemplate jdbcTemplate;
    private final StandardRepository standardRepository;
    private final StandardPracticeRepository standardPracticeRepository;
    private final StandardService standardService;
    private final StandardStatsRepository standardStatsRepository;
    private final PracticeLinkRepository practiceLinkRepository;
    private final PracticeRepository practiceRepository;
    private final PracticeValueLogRepository practiceValueLogRepository;

    @Transactional
    public EntryDTO createEntry(EntryDTO entryDTO, String username) {
        log.info("=== CREATE ENTRY ===");
        log.info("Username: {}", username);
        log.info("Entry data: {}", entryDTO);

        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> {
                        log.error("User not found: {}", username);
                        return new RuntimeException("User not found: " + username);
                    });

            log.info("Found user with id: {}", user.getId());

            CalendarEntry entry = new CalendarEntry();
            entry.setUser(user);
            entry.setCategory(entryDTO.getCategory());
            entry.setPractice(entryDTO.getPractice());

            // Конвертация периода
            Period period;
            switch (entryDTO.getPeriod()) {
                case "morning":
                    period = Period.morning;
                    break;
                case "day":
                    period = Period.day;
                    break;
                case "evening":
                    period = Period.evening;
                    break;
                default:
                    period = Period.morning;
            }
            entry.setPeriod(period);
            log.info("Set period: {}", period);

            // Конвертация интервала повторения
            if (entryDTO.getRepeatInterval() != null && !entryDTO.getRepeatInterval().isEmpty()) {
                entry.setRepeatInterval(RepeatInterval.fromDisplayName(entryDTO.getRepeatInterval()));
                log.info("Set repeat interval: {}", entryDTO.getRepeatInterval());
            } else {
                entry.setRepeatInterval(RepeatInterval.NONE);
                log.info("Set repeat interval: NONE");
            }

            entry.setEntryDate(entryDTO.getEntryDate());
            log.info("Set entry date: {}", entryDTO.getEntryDate());

            CalendarEntry savedEntry = calendarRepository.save(entry);
            log.info("✅ Saved entry with ID: {}, UserID: {}", savedEntry.getId(), savedEntry.getUser().getId());

            return convertToDTO(savedEntry);

        } catch (Exception e) {
            log.error("❌ Error creating entry: {}", e.getMessage(), e);
            throw e;
        }
    }

    public List<EntryDTO> getWeekEntries(String username, LocalDate startDate, LocalDate endDate) {
        log.info("=== GET WEEK ENTRIES ===");
        log.info("Username: {}", username);
        log.info("Date range: {} to {}", startDate, endDate);

        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> {
                        log.error("User not found: {}", username);
                        return new RuntimeException("User not found: " + username);
                    });

            log.info("Found user: id={}, username={}", user.getId(), user.getUsername());

            List<CalendarEntry> entries = calendarRepository
                    .findByUserAndEntryDateBetweenOrderByEntryDateAscPeriodAsc(user, startDate, endDate);

            log.info("Found {} entries", entries.size());

            if (!entries.isEmpty()) {
                CalendarEntry first = entries.get(0);
                log.info("First entry - ID: {}, Category: {}, Practice: {}, Date: {}, UserID: {}",
                        first.getId(), first.getCategory(), first.getPractice(),
                        first.getEntryDate(), first.getUser().getId());
            }

            return entries.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("❌ Error getting week entries: {}", e.getMessage(), e);
            throw e;
        }
    }


    /**
     * Обновленный метод deleteEntry с компенсацией
     */
    // Убираем @Transactional с метода и управляем транзакцией вручную
    public void deleteEntry(Long entryId, String username) {
        log.info("=== DELETE ENTRY ===");
        log.info("Entry ID: {}, Username: {}", entryId, username);

        // Отдельная транзакция для поиска записи
        CalendarEntry entry = findEntrySafely(entryId, username);
        if (entry == null) {
            return;
        }

        // Компенсируем связанные практики в отдельной транзакции
        compensateLinkedPracticesInNewTransaction(entry);

        // Удаляем запись из календаря в отдельной транзакции
        deleteCalendarEntryInNewTransaction(entryId);
    }

    /**
     * Безопасный поиск записи в отдельной транзакции
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CalendarEntry findEntrySafely(Long entryId, String username) {
        try {
            Optional<CalendarEntry> entryOpt = calendarRepository.findById(entryId);

            if (entryOpt.isEmpty()) {
                log.warn("Entry with ID {} not found, might have been already deleted", entryId);
                return null;
            }

            CalendarEntry entry = entryOpt.get();

            if (!entry.getUser().getUsername().equals(username)) {
                log.error("Unauthorized: Entry belongs to {} but user is {}",
                        entry.getUser().getUsername(), username);
                return null;
            }

            return entry;
        } catch (Exception e) {
            log.error("Error finding entry: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Компенсация связанных практик в отдельной транзакции
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void compensateLinkedPracticesInNewTransaction(CalendarEntry entry) {
        log.info("=== COMPENSATE LINKED PRACTICES IN NEW TRANSACTION ===");

        try {
            // Находим все логи, связанные с этим событием
            List<PracticeValueLog> logs = practiceValueLogRepository.findBySourceEntryId(entry.getId());

            if (logs.isEmpty()) {
                log.info("No linked logs found for entry ID: {}", entry.getId());
                return;
            }

            log.info("Found {} logs to compensate", logs.size());

            // Для каждого лога вычитаем значение из целевой практики
            for (PracticeValueLog logEntry : logs) {
                Practice targetPractice = logEntry.getTargetPractice();
                Integer valueToSubtract = logEntry.getValue();
                LocalDate date = logEntry.getEntryDate();

                log.info("Compensating: subtracting {} from {} in category {} on date {}",
                        valueToSubtract, targetPractice.getName(), targetPractice.getCategory().getName(), date);

                subtractFromTargetPracticeSafely(targetPractice, date, valueToSubtract);
            }

            // Удаляем логи после компенсации
            practiceValueLogRepository.deleteBySourceEntryId(entry.getId());
            log.info("Deleted {} logs for entry ID: {}", logs.size(), entry.getId());

        } catch (Exception e) {
            log.error("Error in compensateLinkedPracticesInNewTransaction: {}", e.getMessage(), e);
            // Не пробрасываем исключение
        }
    }

    /**
     * Удаление записи из календаря в отдельной транзакции
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteCalendarEntryInNewTransaction(Long entryId) {
        try {
            calendarRepository.deleteById(entryId);
            log.info("✅ Successfully deleted entry with ID: {}", entryId);
        } catch (Exception e) {
            log.error("Error deleting calendar entry: {}", e.getMessage(), e);
        }
    }

    /**
     * Компенсирует все связанные изменения при удалении события
     */
    private void compensateLinkedPractices(CalendarEntry entry) {
        log.info("=== COMPENSATE LINKED PRACTICES ===");
        log.info("Compensating changes for entry ID: {}", entry.getId());

        // Находим все логи, связанные с этим событием
        List<PracticeValueLog> logs = practiceValueLogRepository.findBySourceEntryId(entry.getId());

        if (logs.isEmpty()) {
            log.info("No linked logs found for entry ID: {}", entry.getId());
            return;
        }

        log.info("Found {} logs to compensate", logs.size());

        // Для каждого лога вычитаем значение из целевой практики
        for (PracticeValueLog logEntry : logs) {
            Practice targetPractice = logEntry.getTargetPractice();
            Integer valueToSubtract = logEntry.getValue();
            LocalDate date = logEntry.getEntryDate();

            log.info("Compensating: subtracting {} from {} in category {} on date {}",
                    valueToSubtract, targetPractice.getName(), targetPractice.getCategory().getName(), date);

            // Используем безопасный метод для вычитания
            subtractFromTargetPracticeSafely(targetPractice, date, valueToSubtract);
        }

        // Удаляем логи после компенсации
        practiceValueLogRepository.deleteBySourceEntryId(entry.getId());
        log.info("Deleted {} logs for entry ID: {}", logs.size(), entry.getId());
    }



    /**
     * Добавляет значение к целевой практике с логированием
     */
    private void addValueToTargetPracticeWithLog(Practice targetPractice, LocalDate date,
                                                 Integer valueToAdd, Long sourceEntryId,
                                                 Practice sourcePractice) {
        log.info("Adding value {} to target practice: {} in category {} on date {} with log",
                valueToAdd, targetPractice.getName(), targetPractice.getCategory().getName(), date);

        String tableName = targetPractice.getCategory().getName().toLowerCase() + "_practices";
        String columnName = getColumnName(targetPractice.getName());

        try {
            // Проверяем существование таблицы и колонки
            ensureTableExists(targetPractice.getCategory().getName());
            ensureColumnExists(tableName, columnName);

            // Получаем текущее значение
            Integer currentValue = getCurrentPracticeValue(tableName, columnName, date);

            // Вычисляем новое значение (суммируем)
            int newValue = (currentValue != null ? currentValue : 0) + valueToAdd;
            log.info("Target current: {}, adding: {}, new: {}", currentValue, valueToAdd, newValue);

            // Сохраняем
            savePracticeValueToTable(tableName, columnName, date, newValue);

            // Создаем запись в логе
            PracticeValueLog logEntry = new PracticeValueLog();
            logEntry.setSourceEntryId(sourceEntryId);
            logEntry.setSourcePractice(sourcePractice);
            logEntry.setTargetPractice(targetPractice);
            logEntry.setValue(valueToAdd);
            logEntry.setEntryDate(date);
            logEntry.setOperationType("ADD");

            practiceValueLogRepository.save(logEntry);
            log.info("Created log entry for addition: sourceEntryId={}, targetPractice={}, value={}",
                    sourceEntryId, targetPractice.getName(), valueToAdd);

        } catch (Exception e) {
            log.error("Error adding value to target practice: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Безопасное вычитание значения из целевой практики
     */
    private void subtractFromTargetPracticeSafely(Practice targetPractice, LocalDate date, Integer valueToSubtract) {
        String tableName = targetPractice.getCategory().getName().toLowerCase() + "_practices";
        String columnName = getColumnName(targetPractice.getName());

        log.info("Subtracting {} from {}.{} on date {}",
                valueToSubtract, tableName, columnName, date);

        try {
            // Проверяем существование таблицы
            if (!tableExistsSafely(tableName)) {
                log.warn("Table {} does not exist, skipping", tableName);
                return;
            }

            // Получаем текущее значение
            Integer currentValue = getCurrentPracticeValueSafely(tableName, columnName, date);

            if (currentValue != null) {
                // Вычитаем значение
                int newValue = currentValue - valueToSubtract;
                log.info("Current value: {}, subtracting: {}, new value: {}",
                        currentValue, valueToSubtract, newValue);

                if (newValue < 0) {
                    log.warn("New value would be negative ({}), setting to 0", newValue);
                    newValue = 0;
                }

                if (newValue == 0) {
                    // Если стало 0, обнуляем
                    resetPracticeValueSafely(tableName, columnName, date);
                } else {
                    // Обновляем с новым значением
                    updatePracticeValueSafely(tableName, columnName, date, newValue);

                    // Проверяем, что значение сохранилось
                    Integer savedValue = getCurrentPracticeValueSafely(tableName, columnName, date);
                    log.info("Verified saved value: {}", savedValue);
                }
            } else {
                log.warn("No current value found for {}.{} on date {}, nothing to subtract",
                        tableName, columnName, date);
            }

        } catch (Exception e) {
            log.error("Error subtracting from target practice {}: {}", targetPractice.getName(), e.getMessage());
        }
    }

    /**
     * Безопасное обновление значения
     */
    private void updatePracticeValueSafely(String tableName, String columnName, LocalDate date, Integer newValue) {
        try {
            String updateQuery = String.format(
                    "UPDATE %s SET %s = ?, updated_at = CURRENT_TIMESTAMP WHERE entry_date = ?",
                    tableName, columnName
            );
            int updated = jdbcTemplate.update(updateQuery, newValue, date);
            log.info("Updated {} to {} for date {}, rows affected: {}", columnName, newValue, date, updated);
        } catch (Exception e) {
            log.error("Error updating practice value: {}", e.getMessage());
        }
    }


    /**
     * Безопасное получение текущего значения
     */
    private Integer getCurrentPracticeValueSafely(String tableName, String columnName, LocalDate date) {
        try {
            String query = String.format("SELECT %s FROM %s WHERE entry_date = ?", columnName, tableName);
            log.debug("Executing query: {}", query);
            Integer value = jdbcTemplate.queryForObject(query, Integer.class, date);
            log.debug("Retrieved value: {}", value);
            return value;
        } catch (EmptyResultDataAccessException e) {
            log.debug("No value found for {}.{} on date {}", tableName, columnName, date);
            return null;
        } catch (Exception e) {
            log.error("Error getting current value: {}", e.getMessage());
            return null;
        }
    }



    /**
     * Безопасное обнуление значения
     */
    private void resetPracticeValueSafely(String tableName, String columnName, LocalDate date) {
        try {
            String updateQuery = String.format(
                    "UPDATE %s SET %s = 0, updated_at = CURRENT_TIMESTAMP WHERE entry_date = ?",
                    tableName, columnName
            );
            jdbcTemplate.update(updateQuery, date);
            log.info("Reset {} to 0 for date {}", columnName, date);

            // Проверяем, не стала ли строка пустой
            cleanupEmptyRowSafely(tableName, date);

        } catch (Exception e) {
            log.error("Error resetting practice value: {}", e.getMessage());
        }
    }


    /**
     * Удаление значений практик без транзакции
     */
    private void deletePracticeValuesNonTransactional(CalendarEntry entry) {
        log.info("=== DELETE PRACTICE VALUES NON-TRANSACTIONAL ===");

        try {
            Category category = categoryRepository.findByName(entry.getCategory())
                    .orElseThrow(() -> new RuntimeException("Category not found: " + entry.getCategory()));

            List<Standard> standards = standardRepository.findByCategoryId(category.getId());
            Standard matchedStandard = standards.stream()
                    .filter(s -> s.getName().equals(entry.getPractice()))
                    .findFirst()
                    .orElse(null);

            if (matchedStandard != null) {
                List<StandardPractice> standardPractices = standardPracticeRepository.findByStandardId(matchedStandard.getId());

                for (StandardPractice sp : standardPractices) {
                    Practice practice = sp.getPractice();

                    // Удаляем значение для каждой практики в отдельном try-catch
                    try {
                        deleteSinglePracticeValueNonTransactional(practice, entry.getEntryDate());
                    } catch (Exception e) {
                        log.error("Error deleting value for practice {}, but continuing: {}", practice.getName(), e.getMessage());
                    }

                    // Проверяем связи
                    try {
                        List<PracticeLink> links = practiceLinkRepository.findBySourcePracticeId(practice.getId());

                        for (PracticeLink link : links) {
                            Practice targetPractice = link.getTargetPractice();
                            try {
                                deleteSinglePracticeValueNonTransactional(targetPractice, entry.getEntryDate());
                            } catch (Exception e) {
                                log.error("Error deleting value for target practice {}, but continuing: {}",
                                        targetPractice.getName(), e.getMessage());
                            }
                        }
                    } catch (Exception e) {
                        log.error("Error checking links for practice {}, but continuing: {}", practice.getName(), e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error in deletePracticeValuesNonTransactional: {}", e.getMessage(), e);
            // Не пробрасываем исключение
        }
    }

    /**
     * Удаление значения одной практики без транзакции
     */
    private void deleteSinglePracticeValueNonTransactional(Practice practice, LocalDate date) {
        String tableName = practice.getCategory().getName().toLowerCase() + "_practices";
        String columnName = getColumnName(practice.getName());

        try {
            // Просто пытаемся обновить, игнорируем ошибки
            String updateQuery = String.format(
                    "UPDATE %s SET %s = 0, updated_at = CURRENT_TIMESTAMP WHERE entry_date = ?",
                    tableName, columnName
            );
            jdbcTemplate.update(updateQuery, date);
            log.debug("Reset {} for date {}", columnName, date);
        } catch (Exception e) {
            log.debug("Could not reset {} for date {}: {}", columnName, date, e.getMessage());
            // Игнорируем ошибку
        }
    }

    /**
     * Безопасное удаление значений практик с отдельной обработкой ошибок
     */
    private void deletePracticeValuesSafely(CalendarEntry entry) {
        log.info("=== DELETE PRACTICE VALUES SAFELY ===");

        try {
            Category category = categoryRepository.findByName(entry.getCategory())
                    .orElseThrow(() -> new RuntimeException("Category not found: " + entry.getCategory()));

            List<Standard> standards = standardRepository.findByCategoryId(category.getId());
            Standard matchedStandard = standards.stream()
                    .filter(s -> s.getName().equals(entry.getPractice()))
                    .findFirst()
                    .orElse(null);

            if (matchedStandard != null) {
                log.info("Found matching standard: {} (ID: {})", matchedStandard.getName(), matchedStandard.getId());

                List<StandardPractice> standardPractices = standardPracticeRepository.findByStandardId(matchedStandard.getId());
                log.info("Standard has {} practices", standardPractices.size());

                for (StandardPractice sp : standardPractices) {
                    Practice practice = sp.getPractice();
                    log.info("Processing practice: {} in category {}", practice.getName(), practice.getCategory().getName());

                    // Удаляем значение для каждой практики в отдельном try-catch
                    try {
                        deleteSinglePracticeValueSafely(practice, entry.getEntryDate());
                    } catch (Exception e) {
                        log.error("Error deleting value for practice {}, continuing: {}", practice.getName(), e.getMessage());
                    }

                    // Проверяем связи
                    try {
                        List<PracticeLink> links = practiceLinkRepository.findBySourcePracticeId(practice.getId());

                        if (!links.isEmpty()) {
                            log.info("Found {} links for practice {}", links.size(), practice.getName());

                            for (PracticeLink link : links) {
                                Practice targetPractice = link.getTargetPractice();
                                log.info("Deleting value from target practice: {} in category {}",
                                        targetPractice.getName(), targetPractice.getCategory().getName());

                                try {
                                    deleteSinglePracticeValueSafely(targetPractice, entry.getEntryDate());
                                } catch (Exception e) {
                                    log.error("Error deleting value for target practice {}, continuing: {}",
                                            targetPractice.getName(), e.getMessage());
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.error("Error checking links for practice {}, continuing: {}", practice.getName(), e.getMessage());
                    }
                }
            } else {
                log.warn("No standard found with name: {}", entry.getPractice());
            }

        } catch (Exception e) {
            log.error("Error in deletePracticeValuesSafely: {}", e.getMessage(), e);
            // Не пробрасываем исключение, чтобы не портить транзакцию
        }
    }

    /**
     * Безопасное удаление значения одной практики
     */
    private void deleteSinglePracticeValueSafely(Practice practice, LocalDate date) {
        log.info("Safely deleting value for practice: {} in category {} on date {}",
                practice.getName(), practice.getCategory().getName(), date);

        String tableName = practice.getCategory().getName().toLowerCase() + "_practices";
        String columnName = getColumnName(practice.getName());

        try {
            // Проверяем существование таблицы
            if (!tableExistsSafely(tableName)) {
                log.warn("Table {} does not exist, skipping", tableName);
                return;
            }

            // Получаем тип колонки
            String columnType = getColumnTypeSafely(tableName, columnName);
            if (columnType == null) {
                log.warn("Column {} does not exist in table {}, skipping", columnName, tableName);
                return;
            }

            // Обнуляем значение
            String updateQuery;
            if (isTimeType(columnType)) {
                updateQuery = String.format(
                        "UPDATE %s SET %s = NULL, updated_at = CURRENT_TIMESTAMP WHERE entry_date = ?",
                        tableName, columnName
                );
            } else {
                updateQuery = String.format(
                        "UPDATE %s SET %s = 0, updated_at = CURRENT_TIMESTAMP WHERE entry_date = ?",
                        tableName, columnName
                );
            }

            jdbcTemplate.update(updateQuery, date);
            log.info("Reset {} for date {}", columnName, date);

            // Проверяем, не стала ли строка пустой
            cleanupEmptyRowSafely(tableName, date);

        } catch (Exception e) {
            log.error("Error in deleteSinglePracticeValueSafely: {}", e.getMessage(), e);
            // Не пробрасываем исключение
        }
    }


    /**
     * Безопасная проверка существования таблицы
     */
    private boolean tableExistsSafely(String tableName) {
        try {
            String query = "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = ?)";
            return Boolean.TRUE.equals(jdbcTemplate.queryForObject(query, Boolean.class, tableName));
        } catch (Exception e) {
            log.error("Error checking if table exists: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Безопасное получение типа колонки
     */
    private String getColumnTypeSafely(String tableName, String columnName) {
        try {
            String query = """
            SELECT data_type 
            FROM information_schema.columns 
            WHERE table_name = ? AND column_name = ?
            """;
            return jdbcTemplate.queryForObject(query, String.class, tableName, columnName);
        } catch (EmptyResultDataAccessException e) {
            return null;
        } catch (Exception e) {
            log.error("Error getting column type: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Безопасная очистка пустых строк
     */
    private void cleanupEmptyRowSafely(String tableName, LocalDate date) {
        try {
            // Получаем все колонки таблицы
            List<Map<String, Object>> columns = getTableColumnsSafely(tableName);

            if (columns.isEmpty()) {
                return;
            }

            StringBuilder checkQuery = new StringBuilder(
                    "SELECT COUNT(*) FROM " + tableName + " WHERE entry_date = ? AND (");

            boolean hasConditions = false;

            for (Map<String, Object> column : columns) {
                String colName = (String) column.get("column_name");
                String dataType = (String) column.get("data_type");

                if (hasConditions) {
                    checkQuery.append(" OR ");
                }

                if (isNumericType(dataType)) {
                    checkQuery.append(colName).append(" != 0 AND ").append(colName).append(" IS NOT NULL");
                } else if (isTimeType(dataType)) {
                    checkQuery.append(colName).append(" IS NOT NULL");
                } else if (isStringType(dataType)) {
                    checkQuery.append(colName).append(" IS NOT NULL AND ").append(colName).append(" != ''");
                }

                hasConditions = true;
            }

            checkQuery.append(")");

            if (!hasConditions) {
                return;
            }

            Integer nonZeroCount = jdbcTemplate.queryForObject(checkQuery.toString(), Integer.class, date);

            if (nonZeroCount == 0) {
                String deleteQuery = "DELETE FROM " + tableName + " WHERE entry_date = ?";
                jdbcTemplate.update(deleteQuery, date);
                log.info("Deleted empty row for date {} from {}", date, tableName);
            }

        } catch (Exception e) {
            log.error("Error cleaning up empty row: {}", e.getMessage());
        }
    }

    /**
     * Безопасное получение колонок таблицы
     */
    private List<Map<String, Object>> getTableColumnsSafely(String tableName) {
        try {
            String query = """
            SELECT column_name, data_type 
            FROM information_schema.columns 
            WHERE table_name = ? 
            AND column_name NOT IN ('id', 'entry_date', 'created_at', 'updated_at')
            """;
            return jdbcTemplate.queryForList(query, tableName);
        } catch (Exception e) {
            log.error("Error getting table columns: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Обновленный метод fillStandardValuesWithLinks с логированием
     */
    private void fillStandardValuesWithLinks(CalendarEntry entry, Standard standard) {
        log.info("=== FILL STANDARD VALUES WITH LINKS ===");
        log.info("Filling standard values with links for entry: {}, standard: {}", entry.getId(), standard.getName());

        try {
            // Получаем все практики из стандарта
            List<StandardPractice> standardPractices = standardPracticeRepository.findByStandardId(standard.getId());
            log.info("Found {} practices in standard", standardPractices.size());

            for (StandardPractice sp : standardPractices) {
                if (Boolean.TRUE.equals(sp.getIsActive())) {
                    Practice practice = sp.getPractice();
                    Integer targetValue = sp.getTargetValue();

                    log.info("Processing practice: {} in category {}", practice.getName(), practice.getCategory().getName());

                    // Сохраняем значение в исходную практику
                    saveSinglePracticeValue(practice, entry.getEntryDate(), targetValue);

                    // Проверяем, есть ли связи, где эта практика является источником
                    List<PracticeLink> links = practiceLinkRepository.findBySourcePracticeId(practice.getId());

                    if (!links.isEmpty()) {
                        log.info("Found {} links for practice {}", links.size(), practice.getName());

                        for (PracticeLink link : links) {
                            Practice targetPractice = link.getTargetPractice();
                            log.info("Adding value to target practice: {} in category {}",
                                    targetPractice.getName(), targetPractice.getCategory().getName());

                            // Добавляем значение к целевой практике с логированием
                            addValueToTargetPracticeWithLog(
                                    targetPractice,
                                    entry.getEntryDate(),
                                    targetValue,
                                    entry.getId(),
                                    practice
                            );
                        }
                    }
                }
            }

            // Записываем статистику выполнения стандарта
            standardService.recordStandardExecution(standard.getId(), entry.getEntryDate());
            log.info("✅ Successfully filled standard values with links for date: {}", entry.getEntryDate());

        } catch (Exception e) {
            log.error("❌ Error filling standard values with links: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Сохраняет значение для одной практики
     */
    private void saveSinglePracticeValue(Practice practice, LocalDate date, Integer targetValue) {
        log.info("Saving value for practice: {} in category {} on date {}",
                practice.getName(), practice.getCategory().getName(), date);

        String tableName = practice.getCategory().getName().toLowerCase() + "_practices";
        String columnName = getColumnName(practice.getName());

        try {
            // Проверяем существование таблицы и колонки
            ensureTableExists(practice.getCategory().getName());
            ensureColumnExists(tableName, columnName);

            // Получаем текущее значение
            Integer currentValue = getCurrentPracticeValue(tableName, columnName, date);

            // Вычисляем новое значение (суммируем)
            int newValue = (currentValue != null ? currentValue : 0) + targetValue;
            log.info("Current: {}, adding: {}, new: {}", currentValue, targetValue, newValue);

            // Сохраняем
            savePracticeValueToTable(tableName, columnName, date, newValue);

        } catch (Exception e) {
            log.error("Error saving practice value: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Добавляет значение к целевой практике (суммирует)
     */
    private void addValueToTargetPractice(Practice targetPractice, LocalDate date, Integer valueToAdd) {
        log.info("Adding value {} to target practice: {} in category {} on date {}",
                valueToAdd, targetPractice.getName(), targetPractice.getCategory().getName(), date);

        String tableName = targetPractice.getCategory().getName().toLowerCase() + "_practices";
        String columnName = getColumnName(targetPractice.getName());

        try {
            // Проверяем существование таблицы и колонки
            ensureTableExists(targetPractice.getCategory().getName());
            ensureColumnExists(tableName, columnName);

            // Получаем текущее значение
            Integer currentValue = getCurrentPracticeValue(tableName, columnName, date);

            // Вычисляем новое значение (суммируем)
            int newValue = (currentValue != null ? currentValue : 0) + valueToAdd;
            log.info("Target current: {}, adding: {}, new: {}", currentValue, valueToAdd, newValue);

            // Сохраняем
            savePracticeValueToTable(tableName, columnName, date, newValue);

        } catch (Exception e) {
            log.error("Error adding value to target practice: {}", e.getMessage(), e);
            throw e;
        }
    }


    @Transactional
    public void updateEntryStatus(Long entryId, EntryStatusDTO statusDTO, String username) {
        log.info("=== UPDATE ENTRY STATUS START ===");
        log.info("Entry ID: {}", entryId);
        log.info("Status: {}", statusDTO.getStatus());
        log.info("Username: {}", username);

        try {
            CalendarEntry entry = calendarRepository.findById(entryId)
                    .orElseThrow(() -> new RuntimeException("Entry not found with id: " + entryId));

            log.info("Found entry: category={}, practice={}, date={}",
                    entry.getCategory(), entry.getPractice(), entry.getEntryDate());
            log.info("Entry user: {}", entry.getUser().getUsername());
            log.info("Entry user ID: {}", entry.getUser().getId());

            // Проверяем, что запись принадлежит пользователю
            if (!entry.getUser().getUsername().equals(username)) {
                log.error("Unauthorized: Entry belongs to {} but user is {}",
                        entry.getUser().getUsername(), username);
                throw new RuntimeException("Unauthorized: Entry belongs to different user");
            }

            // Обновляем статус в самой записи
            entry.setStatus(statusDTO.getStatus());
            calendarRepository.save(entry);
            log.info("Updated calendar entry status to: {}", statusDTO.getStatus());

            // Обновляем или создаем детальную запись статуса
            EntryStatus entryStatus = entry.getEntryStatus();
            if (entryStatus == null) {
                entryStatus = new EntryStatus();
                entryStatus.setCalendarEntry(entry);
                log.info("Creating new EntryStatus record");
            } else {
                log.info("Updating existing EntryStatus record");
            }

            entryStatus.setStatus(statusDTO.getStatus());
            entryStatus.setNotes(statusDTO.getNotes());

            entryStatusRepository.save(entryStatus);
            log.info("Saved EntryStatus record");

            // Если статус "completed", заполняем значения стандарта
            if ("completed".equals(statusDTO.getStatus())) {
                log.info("STATUS IS COMPLETED - ATTEMPTING TO FILL STANDARD VALUES");

                try {
                    Category category = categoryRepository.findByName(entry.getCategory())
                            .orElseThrow(() -> new RuntimeException("Category not found: " + entry.getCategory()));

                    List<Standard> standards = standardRepository.findByCategoryId(category.getId());

                    Standard matchedStandard = standards.stream()
                            .filter(s -> s.getName().equals(entry.getPractice()))
                            .findFirst()
                            .orElse(null);

                    if (matchedStandard != null) {
                        log.info("Found matching standard: {} (ID: {})", matchedStandard.getName(), matchedStandard.getId());
                        fillStandardValuesWithLinks(entry, matchedStandard); // Вместо fillStandardValues
                    } else {
                        log.warn("No standard found with name: {}", entry.getPractice());
                    }
                } catch (Exception e) {
                    log.error("Error processing standard: {}", e.getMessage(), e);
                }
            }

            log.info("✅ Successfully updated status for entry ID: {}", entryId);
            log.info("=== UPDATE ENTRY STATUS END ===");

        } catch (Exception e) {
            log.error("❌ Error in updateEntryStatus: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Заполняет нормативные значения согласно стандарту
     */
    private void fillStandardValues(CalendarEntry entry, Standard standard) {
        log.info("=== FILL STANDARD VALUES ===");
        log.info("Filling standard values for entry: {}, standard: {}", entry.getId(), standard.getName());

        try {
            String tableName = entry.getCategory().toLowerCase() + "_practices";
            LocalDate entryDate = entry.getEntryDate();

            log.info("Table: {}, Date: {}", tableName, entryDate);

            // Получаем все практики из стандарта
            List<StandardPractice> standardPractices = standardPracticeRepository.findByStandardId(standard.getId());
            log.info("Found {} practices in standard", standardPractices.size());

            for (StandardPractice sp : standardPractices) {
                if (Boolean.TRUE.equals(sp.getIsActive())) {
                    String practiceName = sp.getPractice().getName();
                    String columnName = getColumnName(practiceName);
                    Integer targetValue = sp.getTargetValue();

                    log.info("Setting practice '{}' to target value: {} for date {}",
                            practiceName, targetValue, entryDate);

                    // Проверяем, существует ли таблица
                    ensureTableExists(entry.getCategory());
                    ensureColumnExists(tableName, columnName);

                    // Получаем текущее значение
                    Integer currentValue = getCurrentPracticeValue(tableName, columnName, entryDate);

                    // Вычисляем новое значение (суммируем)
                    int newValue = (currentValue != null ? currentValue : 0) + targetValue;
                    log.info("Current: {}, adding: {}, new: {}", currentValue, targetValue, newValue);

                    // Сохраняем
                    savePracticeValueToTable(tableName, columnName, entryDate, newValue);
                }
            }

            // Записываем статистику выполнения стандарта
            standardService.recordStandardExecution(standard.getId(), entry.getEntryDate());
            log.info("✅ Successfully filled standard values for date: {}", entryDate);

        } catch (Exception e) {
            log.error("❌ Error filling standard values: {}", e.getMessage(), e);
            throw e; // Пробрасываем исключение для отката транзакции
        }
    }

    /**
     * Удаляет значения практик из динамических таблиц
     */
    private void deletePracticeValues(CalendarEntry entry) {
        log.info("=== DELETE PRACTICE VALUES ===");

        try {
            Category category = categoryRepository.findByName(entry.getCategory())
                    .orElseThrow(() -> new RuntimeException("Category not found: " + entry.getCategory()));

            List<Standard> standards = standardRepository.findByCategoryId(category.getId());
            Standard matchedStandard = standards.stream()
                    .filter(s -> s.getName().equals(entry.getPractice()))
                    .findFirst()
                    .orElse(null);

            if (matchedStandard != null) {
                log.info("Found matching standard: {} (ID: {})", matchedStandard.getName(), matchedStandard.getId());

                List<StandardPractice> standardPractices = standardPracticeRepository.findByStandardId(matchedStandard.getId());
                log.info("Standard has {} practices", standardPractices.size());

                for (StandardPractice sp : standardPractices) {
                    Practice practice = sp.getPractice();
                    log.info("Processing practice: {} in category {}", practice.getName(), practice.getCategory().getName());

                    deleteSinglePracticeValue(practice, entry.getEntryDate());

                    List<PracticeLink> links = practiceLinkRepository.findBySourcePracticeId(practice.getId());

                    if (!links.isEmpty()) {
                        log.info("Found {} links for practice {}", links.size(), practice.getName());

                        for (PracticeLink link : links) {
                            Practice targetPractice = link.getTargetPractice();
                            log.info("Deleting value from target practice: {} in category {}",
                                    targetPractice.getName(), targetPractice.getCategory().getName());

                            deleteSinglePracticeValue(targetPractice, entry.getEntryDate());
                        }
                    }
                }
            } else {
                log.warn("No standard found with name: {}", entry.getPractice());
            }

        } catch (Exception e) {
            log.error("Error deleting practice values: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Удаляет значение одной практики за конкретную дату
     */
    private void deleteSinglePracticeValue(Practice practice, LocalDate date) {
        log.info("Deleting value for practice: {} in category {} on date {}",
                practice.getName(), practice.getCategory().getName(), date);

        String tableName = practice.getCategory().getName().toLowerCase() + "_practices";
        String columnName = getColumnName(practice.getName());

        try {
            if (!tableExists(tableName)) {
                log.warn("Table {} does not exist, nothing to delete", tableName);
                return;
            }

            String columnType = getColumnType(tableName, columnName);

            String updateQuery;
            if ("time".equals(columnType)) {
                updateQuery = String.format(
                        "UPDATE %s SET %s = NULL, updated_at = CURRENT_TIMESTAMP WHERE entry_date = ?",
                        tableName, columnName
                );
            } else {
                updateQuery = String.format(
                        "UPDATE %s SET %s = 0, updated_at = CURRENT_TIMESTAMP WHERE entry_date = ?",
                        tableName, columnName
                );
            }

            int updated = jdbcTemplate.update(updateQuery, date);
            log.info("Reset {} for date {}, updated {} rows", columnName, date, updated);

            cleanupEmptyRow(tableName, date);

        } catch (Exception e) {
            log.error("Error deleting practice value: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Проверяет, не стала ли строка полностью пустой, и удаляет её если да
     */
    private void cleanupEmptyRow(String tableName, LocalDate date) {
        try {
            String checkQuery = String.format(
                    "SELECT COUNT(*) FROM %s WHERE entry_date = ? AND " +
                            "(meditation != 0 OR plank != 0 OR squat != 0 OR concentration != 0 OR pranayama != 0)",
                    tableName
            );

            Integer nonZeroCount = jdbcTemplate.queryForObject(checkQuery, Integer.class, date);

            if (nonZeroCount == 0) {
                String deleteQuery = "DELETE FROM " + tableName + " WHERE entry_date = ?";
                jdbcTemplate.update(deleteQuery, date);
                log.info("Deleted empty row for date {} from {}", date, tableName);
            }
        } catch (Exception e) {
            log.error("Error cleaning up empty row: {}", e.getMessage());
        }
    }

    /**
     * Проверяет существование таблицы
     */
    private boolean tableExists(String tableName) {
        String query = "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = ?)";
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(query, Boolean.class, tableName));
    }

    /**
     * Получает тип колонки
     */
    private String getColumnType(String tableName, String columnName) {
        String query = """
            SELECT data_type 
            FROM information_schema.columns 
            WHERE table_name = ? AND column_name = ?
            """;
        try {
            return jdbcTemplate.queryForObject(query, String.class, tableName, columnName);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    /**
     * Получает список колонок таблицы с их типами
     */
    private List<Map<String, Object>> getTableColumns(String tableName) {
        String query = """
            SELECT column_name, data_type 
            FROM information_schema.columns 
            WHERE table_name = ? 
            AND column_name NOT IN ('id', 'entry_date', 'created_at', 'updated_at')
            """;
        return jdbcTemplate.queryForList(query, tableName);
    }

    /**
     * Проверяет, является ли тип числовым
     */
    private boolean isNumericType(String dataType) {
        return dataType.equals("integer") || dataType.equals("bigint") || dataType.equals("smallint");
    }

    /**
     * Проверяет, является ли тип временным
     */
    private boolean isTimeType(String dataType) {
        return dataType.equals("time") || dataType.equals("timestamp") || dataType.equals("date");
    }

    /**
     * Проверяет, является ли тип строковым
     */
    private boolean isStringType(String dataType) {
        return dataType.equals("character varying") || dataType.equals("text");
    }

    /**
     * Проверяет существование таблицы и создает ее при необходимости
     */
    private void ensureTableExists(String categoryName) {
        String tableName = categoryName.toLowerCase() + "_practices";
        String checkTableQuery = "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = ?)";
        Boolean tableExists = jdbcTemplate.queryForObject(checkTableQuery, Boolean.class, tableName);

        if (!tableExists) {
            log.warn("Table {} does not exist, creating...", tableName);
            dynamicTableService.createCategoryTable(categoryName);
        }
    }

    /**
     * Проверяет существование колонки и создает ее при необходимости
     */
    private void ensureColumnExists(String tableName, String columnName) {
        String checkColumnQuery = "SELECT EXISTS (SELECT FROM information_schema.columns WHERE table_name = ? AND column_name = ?)";
        Boolean columnExists = jdbcTemplate.queryForObject(checkColumnQuery, Boolean.class, tableName, columnName);

        if (!columnExists) {
            log.warn("Column {} does not exist in table {}, creating...", columnName, tableName);
            String columnType = "INTEGER";
            String addColumnQuery = String.format("ALTER TABLE %s ADD COLUMN %s %s", tableName, columnName, columnType);
            jdbcTemplate.execute(addColumnQuery);
            log.info("Column created: {}", columnName);
        }
    }

    /**
     * Получает текущее значение практики из таблицы
     */
    private Integer getCurrentPracticeValue(String tableName, String columnName, LocalDate date) {
        try {
            String query = String.format("SELECT %s FROM %s WHERE entry_date = ?", columnName, tableName);
            return jdbcTemplate.queryForObject(query, Integer.class, date);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    /**
     * Сохраняет значение в таблицу
     */
    private void savePracticeValueToTable(String tableName, String columnName, LocalDate date, Integer value) {
        String checkQuery = String.format("SELECT COUNT(*) FROM %s WHERE entry_date = ?", tableName);
        Integer count = jdbcTemplate.queryForObject(checkQuery, Integer.class, date);

        if (count != null && count > 0) {
            String updateQuery = String.format(
                    "UPDATE %s SET %s = ?, updated_at = CURRENT_TIMESTAMP WHERE entry_date = ?",
                    tableName, columnName
            );
            jdbcTemplate.update(updateQuery, value, date);
            log.info("Updated {} to {}", columnName, value);
        } else {
            String insertQuery = String.format(
                    "INSERT INTO %s (entry_date, %s, created_at, updated_at) VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                    tableName, columnName
            );
            jdbcTemplate.update(insertQuery, date, value);
            log.info("Inserted {} with value {}", columnName, value);
        }
    }

    /**
     * Получает имя колонки из названия практики
     */
    private String getColumnName(String practiceName) {
        if (practiceName == null || practiceName.trim().isEmpty()) {
            return "";
        }

        String result = practiceName.toLowerCase()
                .trim()
                .replaceAll("\\s+", "_")
                .replaceAll("[^a-z0-9а-яё_]", "")
                .replaceAll("_+", "_");

        if (result.isEmpty()) {
            String translit = practiceName.toLowerCase()
                    .replace("а", "a").replace("б", "b").replace("в", "v")
                    .replace("г", "g").replace("д", "d").replace("е", "e")
                    .replace("ё", "e").replace("ж", "zh").replace("з", "z")
                    .replace("и", "i").replace("й", "y").replace("к", "k")
                    .replace("л", "l").replace("м", "m").replace("н", "n")
                    .replace("о", "o").replace("п", "p").replace("р", "r")
                    .replace("с", "s").replace("т", "t").replace("у", "u")
                    .replace("ф", "f").replace("х", "kh").replace("ц", "ts")
                    .replace("ч", "ch").replace("ш", "sh").replace("щ", "sch")
                    .replace("ъ", "").replace("ы", "y").replace("ь", "")
                    .replace("э", "e").replace("ю", "yu").replace("я", "ya")
                    .replaceAll("[^a-z0-9]", "_");

            result = translit.replaceAll("_+", "_");

            if (result.isEmpty()) {
                result = "practice_" + System.currentTimeMillis();
            }
        }

        log.debug("Generated column name '{}' from practice name '{}'", result, practiceName);
        return result;
    }

    public List<PracticeStandard> getStandardsByCategory(String categoryName) {
        log.info("Getting standards for category: {}", categoryName);

        try {
            Category category = categoryRepository.findByName(categoryName)
                    .orElseThrow(() -> new RuntimeException("Category not found: " + categoryName));
            return practiceStandardRepository.findByCategoryId(category.getId());

        } catch (Exception e) {
            log.error("❌ Error getting standards: {}", e.getMessage(), e);
            throw new RuntimeException("Error getting standards: " + e.getMessage(), e);
        }
    }

    private EntryDTO convertToDTO(CalendarEntry entry) {
        EntryDTO dto = new EntryDTO();
        dto.setId(entry.getId());
        dto.setCategory(entry.getCategory());
        dto.setPractice(entry.getPractice());
        dto.setStatus(entry.getStatus());

        String periodCode;
        switch (entry.getPeriod()) {
            case morning:
                periodCode = "morning";
                break;
            case day:
                periodCode = "day";
                break;
            case evening:
                periodCode = "evening";
                break;
            default:
                periodCode = "morning";
        }
        dto.setPeriod(periodCode);

        if (entry.getRepeatInterval() != null) {
            dto.setRepeatInterval(entry.getRepeatInterval().getDisplayName());
        } else {
            dto.setRepeatInterval("Не повторять");
        }

        dto.setEntryDate(entry.getEntryDate());
        return dto;
    }
}