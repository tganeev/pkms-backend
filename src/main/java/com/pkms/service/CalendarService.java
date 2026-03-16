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
import java.util.stream.Collectors;
import org.springframework.dao.EmptyResultDataAccessException;

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
    // Новые зависимости для связанных практик
    private final PracticeLinkRepository practiceLinkRepository;
    private final PracticeRepository practiceRepository;

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

    @Transactional
    public void deleteEntry(Long entryId, String username) {
        log.info("=== DELETE ENTRY ===");
        log.info("Entry ID: {}, Username: {}", entryId, username);

        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            calendarRepository.deleteByUserIdAndId(user.getId(), entryId);
            log.info("✅ Deleted entry with ID: {}", entryId);

        } catch (Exception e) {
            log.error("❌ Error deleting entry: {}", e.getMessage(), e);
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

                        // Получаем все практики из стандарта
                        List<StandardPractice> standardPractices = standardPracticeRepository.findByStandardId(matchedStandard.getId());
                        log.info("Standard has {} practices", standardPractices.size());

                        // Для каждой практики в стандарте проверяем связи
                        for (StandardPractice sp : standardPractices) {
                            Practice practice = sp.getPractice();
                            log.info("Processing practice: {} in category {}", practice.getName(), practice.getCategory().getName());

                            // Проверяем, есть ли связи, где эта практика является источником
                            List<PracticeLink> links = practiceLinkRepository.findBySourcePracticeId(practice.getId());

                            if (!links.isEmpty()) {
                                log.info("Found {} links for practice {}", links.size(), practice.getName());

                                // Сохраняем значение в исходную практику (Core.Meditation)
                                fillStandardValuesForPractice(entry, matchedStandard, practice);

                                // Получаем значение для добавления
                                Integer valueToAdd = getTargetValueForPractice(matchedStandard, practice);
                                log.info("Value to add for target: {}", valueToAdd);

                                // Обрабатываем все связанные практики (Yoga.Meditation)
                                for (PracticeLink link : links) {
                                    Practice targetPractice = link.getTargetPractice();
                                    log.info("Calling addValueToTargetPractice for {} in category {}",
                                            targetPractice.getName(), targetPractice.getCategory().getName());

                                    addValueToTargetPractice(targetPractice, entry.getEntryDate(), valueToAdd);
                                }
                            } else {
                                // Если нет связей, просто сохраняем значение
                                log.info("No links for practice {}, saving directly", practice.getName());
                                fillStandardValuesForPractice(entry, matchedStandard, practice);
                            }
                        }
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
     * Получает целевое значение для практики из стандарта
     */
    private Integer getTargetValueForPractice(Standard standard, Practice practice) {
        return standard.getStandardPractices().stream()
                .filter(sp -> sp.getPractice().getId().equals(practice.getId()))
                .map(StandardPractice::getTargetValue)
                .findFirst()
                .orElse(0);
    }

    /**
     * Заполняет значение для конкретной практики из стандарта
     */
    private void fillStandardValuesForPractice(CalendarEntry entry, Standard standard, Practice practice) {
        log.info("Filling values for practice: {} in category {}",
                practice.getName(), practice.getCategory().getName());

        String tableName = practice.getCategory().getName().toLowerCase() + "_practices";
        String columnName = getColumnName(practice.getName());
        LocalDate entryDate = entry.getEntryDate();

        Integer targetValue = getTargetValueForPractice(standard, practice);

        if (targetValue == 0) {
            log.warn("No target value found for practice {}", practice.getName());
            return;
        }

        // Проверяем существование таблицы и колонки
        ensureTableExists(practice.getCategory().getName());
        ensureColumnExists(tableName, columnName);

        // Получаем текущее значение
        Integer currentValue = getCurrentPracticeValue(tableName, columnName, entryDate);

        // Суммируем
        int newValue = (currentValue != null ? currentValue : 0) + targetValue;
        log.info("Current: {}, adding: {}, new: {}", currentValue, targetValue, newValue);

        // Сохраняем
        savePracticeValueToTable(tableName, columnName, entryDate, newValue);
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
                    String checkTableQuery = "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = ?)";
                    Boolean tableExists = jdbcTemplate.queryForObject(checkTableQuery, Boolean.class, tableName);

                    if (!tableExists) {
                        log.warn("Table {} does not exist, creating...", tableName);
                        dynamicTableService.createCategoryTable(entry.getCategory());
                    }

                    // Проверяем, существует ли колонка
                    String checkColumnQuery = "SELECT EXISTS (SELECT FROM information_schema.columns WHERE table_name = ? AND column_name = ?)";
                    Boolean columnExists = jdbcTemplate.queryForObject(checkColumnQuery, Boolean.class, tableName, columnName);

                    if (!columnExists) {
                        log.warn("Column {} does not exist in table {}, creating...", columnName, tableName);
                        String columnType = "INTEGER";
                        String addColumnQuery = String.format("ALTER TABLE %s ADD COLUMN %s %s", tableName, columnName, columnType);
                        jdbcTemplate.execute(addColumnQuery);
                        log.info("Column created: {}", columnName);
                    }

                    // Вставляем или обновляем данные
                    String checkQuery = String.format("SELECT COUNT(*) FROM %s WHERE entry_date = ?", tableName);
                    Integer count = jdbcTemplate.queryForObject(checkQuery, Integer.class, entryDate);

                    if (count != null && count > 0) {
                        String updateQuery = String.format(
                                "UPDATE %s SET %s = ?, updated_at = CURRENT_TIMESTAMP WHERE entry_date = ?",
                                tableName, columnName
                        );
                        int updated = jdbcTemplate.update(updateQuery, targetValue, entryDate);
                        log.info("Updated {} rows for column {}", updated, columnName);
                    } else {
                        String insertQuery = String.format(
                                "INSERT INTO %s (entry_date, %s, created_at, updated_at) VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                                tableName, columnName
                        );
                        int inserted = jdbcTemplate.update(insertQuery, entryDate, targetValue);
                        log.info("Inserted {} rows for column {}", inserted, columnName);
                    }

                    // Проверяем, что данные сохранились
                    String verifyQuery = String.format("SELECT %s FROM %s WHERE entry_date = ?", columnName, tableName);
                    try {
                        Object savedValue = jdbcTemplate.queryForObject(verifyQuery, Object.class, entryDate);
                        log.info("Verified saved value for {}: {}", columnName, savedValue);
                    } catch (EmptyResultDataAccessException e) {
                        log.warn("Could not verify saved value for {}", columnName);
                    }
                }
            }

            // Записываем статистику выполнения стандарта
            standardService.recordStandardExecution(standard.getId(), entry.getEntryDate());

            log.info("✅ Successfully filled standard values for date: {}", entryDate);

        } catch (Exception e) {
            log.error("❌ Error filling standard values: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Сохраняет значение практики и всех связанных с ней практик (где текущая является источником)
     */
    private void savePracticeValueWithLinks(CalendarEntry entry, Standard matchedStandard, Practice sourcePractice) {
        log.info("=== SAVE PRACTICE WITH LINKS ===");
        log.info("Source practice: {} in category {}", sourcePractice.getName(), sourcePractice.getCategory().getName());

        try {
            // Сначала сохраняем значение в исходную практику (Core)
            fillStandardValues(entry, matchedStandard);

            // Находим все связанные практики, для которых текущая является источником
            List<PracticeLink> links = practiceLinkRepository.findBySourcePracticeId(sourcePractice.getId());
            log.info("Found {} linked target practices for source practice ID: {}", links.size(), sourcePractice.getId());

            for (PracticeLink link : links) {
                Practice targetPractice = link.getTargetPractice();
                log.info("Adding value to target practice: {} in category {}",
                        targetPractice.getName(), targetPractice.getCategory().getName());

                // Находим стандарт для целевой практики (Yoga.Meditation)
                List<Standard> targetStandards = standardRepository.findByCategoryId(targetPractice.getCategory().getId());

                Standard targetStandard = targetStandards.stream()
                        .filter(s -> s.getName().equals(targetPractice.getName()))
                        .findFirst()
                        .orElse(null);

                if (targetStandard != null) {
                    log.info("Found standard for target practice: {} with ID: {}",
                            targetStandard.getName(), targetStandard.getId());

                    // Получаем значение из стандарта источника для этой практики
                    Integer sourceValue = matchedStandard.getStandardPractices().stream()
                            .filter(sp -> sp.getPractice().getId().equals(sourcePractice.getId()))
                            .map(StandardPractice::getTargetValue)
                            .findFirst()
                            .orElse(0);

                    log.info("Source value to add: {}", sourceValue);

                    // Добавляем значение к целевой практике
                    addValueToTargetPractice(targetPractice, entry.getEntryDate(), sourceValue);

                } else {
                    log.warn("No standard found for target practice: {}", targetPractice.getName());
                }
            }

        } catch (Exception e) {
            log.error("Error saving practice with links: {}", e.getMessage(), e);
        }
    }

    /**
     * Добавляет значение к целевой практике (суммирует с существующим)
     */
    private void addValueToTargetPractice(Practice targetPractice, LocalDate entryDate, Integer valueToAdd) {
        log.info("=== ADDING TO TARGET PRACTICE ===");
        log.info("Target practice: {} in category {}", targetPractice.getName(), targetPractice.getCategory().getName());
        log.info("Value to add: {} for date {}", valueToAdd, entryDate);

        // Проверяем, что valueToAdd не null
        if (valueToAdd == null) {
            log.error("Value to add is null!");
            return;
        }

        String tableName = targetPractice.getCategory().getName().toLowerCase() + "_practices";
        String columnName = getColumnName(targetPractice.getName());

        log.info("Table: {}, Column: {}", tableName, columnName);

        try {
            // Проверяем существование таблицы
            log.info("Checking if table {} exists", tableName);
            String checkTableQuery = "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = ?)";
            Boolean tableExists = jdbcTemplate.queryForObject(checkTableQuery, Boolean.class, tableName);
            log.info("Table exists: {}", tableExists);

            if (!tableExists) {
                log.warn("Table {} does not exist, creating...", tableName);
                dynamicTableService.createCategoryTable(targetPractice.getCategory().getName());
                // Проверяем еще раз
                tableExists = jdbcTemplate.queryForObject(checkTableQuery, Boolean.class, tableName);
                log.info("After creation, table exists: {}", tableExists);
            }

            // Проверяем существование колонки
            log.info("Checking if column {} exists in table {}", columnName, tableName);
            String checkColumnQuery = "SELECT EXISTS (SELECT FROM information_schema.columns WHERE table_name = ? AND column_name = ?)";
            Boolean columnExists = jdbcTemplate.queryForObject(checkColumnQuery, Boolean.class, tableName, columnName);
            log.info("Column exists: {}", columnExists);

            if (!columnExists) {
                log.warn("Column {} does not exist in table {}, creating...", columnName, tableName);
                String addColumnQuery = String.format("ALTER TABLE %s ADD COLUMN %s INTEGER", tableName, columnName);
                jdbcTemplate.execute(addColumnQuery);
                log.info("Column created: {}", columnName);
            }

            // Получаем текущее значение
            log.info("Getting current value for date {}", entryDate);
            Integer currentValue = null;
            try {
                String query = String.format("SELECT %s FROM %s WHERE entry_date = ?", columnName, tableName);
                currentValue = jdbcTemplate.queryForObject(query, Integer.class, entryDate);
                log.info("Current value: {}", currentValue);
            } catch (EmptyResultDataAccessException e) {
                log.info("No existing value for date {}", entryDate);
                currentValue = null;
            }

            // Вычисляем новое значение
            int newValue = (currentValue != null ? currentValue : 0) + valueToAdd;
            log.info("New value will be: {}", newValue);

            // Сохраняем
            String checkQuery = String.format("SELECT COUNT(*) FROM %s WHERE entry_date = ?", tableName);
            Integer count = jdbcTemplate.queryForObject(checkQuery, Integer.class, entryDate);

            if (count != null && count > 0) {
                String updateQuery = String.format(
                        "UPDATE %s SET %s = ?, updated_at = CURRENT_TIMESTAMP WHERE entry_date = ?",
                        tableName, columnName
                );
                int updated = jdbcTemplate.update(updateQuery, newValue, entryDate);
                log.info("Updated {} rows in {}", updated, tableName);
            } else {
                String insertQuery = String.format(
                        "INSERT INTO %s (entry_date, %s, created_at, updated_at) VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                        tableName, columnName
                );
                int inserted = jdbcTemplate.update(insertQuery, entryDate, newValue);
                log.info("Inserted {} rows into {}", inserted, tableName);
            }

            // Проверяем, что сохранилось
            try {
                String verifyQuery = String.format("SELECT %s FROM %s WHERE entry_date = ?", columnName, tableName);
                Integer savedValue = jdbcTemplate.queryForObject(verifyQuery, Integer.class, entryDate);
                log.info("✅ Verified saved value in target: {}", savedValue);
            } catch (EmptyResultDataAccessException e) {
                log.error("❌ Could not verify saved value!");
            }

        } catch (Exception e) {
            log.error("❌ Error in addValueToTargetPractice: {}", e.getMessage(), e);
            log.error("Stack trace:", e);
        }
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
        } else {
            log.debug("Table {} exists", tableName);
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
            String addColumnQuery = String.format("ALTER TABLE %s ADD COLUMN %s INTEGER", tableName, columnName);
            jdbcTemplate.execute(addColumnQuery);
            log.info("Column created: {}", columnName);
        } else {
            log.debug("Column {} exists in table {}", columnName, tableName);
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
            return null; // Нет записи на эту дату
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

        // Если после всех преобразований строка пустая, используем транслитерацию
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

        // Маппинг периода
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

        // Маппинг интервала повторения
        if (entry.getRepeatInterval() != null) {
            dto.setRepeatInterval(entry.getRepeatInterval().getDisplayName());
        } else {
            dto.setRepeatInterval("Не повторять");
        }

        dto.setEntryDate(entry.getEntryDate());
        return dto;
    }
}