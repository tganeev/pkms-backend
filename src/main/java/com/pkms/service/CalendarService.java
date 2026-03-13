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
            entry.setDuration(entryDTO.getDuration());

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

            // Если статус "completed", сохраняем значение в динамическую таблицу
            if ("completed".equals(statusDTO.getStatus())) {
                log.info("STATUS IS COMPLETED - ATTEMPTING TO SAVE TO DYNAMIC TABLE");
                try {
                    savePracticeValueToDynamicTable(entry);
                    log.info("✅ SUCCESSFULLY saved to dynamic table");
                } catch (Exception e) {
                    log.error("❌ FAILED to save to dynamic table: {}", e.getMessage(), e);
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
     * Сохраняет значение практики в динамическую таблицу категории
     */
    private void savePracticeValueToDynamicTable(CalendarEntry entry) {
        log.info("=== SAVE TO DYNAMIC TABLE (FIXED VERSION) ===");

        try {
            String tableName = entry.getCategory().toLowerCase() + "_practices";
            String columnName = entry.getPractice().toLowerCase().replace(" ", "_");
            String dateStr = entry.getEntryDate().toString();
            String value = entry.getDuration();

            log.info("Table: {}, Column: {}, Date: {}, Value: {}",
                    tableName, columnName, dateStr, value);

            // Извлекаем числовое значение
            Integer numericValue = null;
            try {
                numericValue = Integer.parseInt(value.replaceAll("[^0-9]", ""));
                log.info("Parsed numeric value: {}", numericValue);
            } catch (Exception e) {
                log.warn("Could not parse numeric value: {}, storing as string", value);
            }

            // Проверяем существование таблицы
            String checkTableQuery = "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = ?)";
            Boolean tableExists = jdbcTemplate.queryForObject(checkTableQuery, Boolean.class, tableName);
            log.info("Table exists: {}", tableExists);

            if (!tableExists) {
                log.warn("Table {} does not exist, creating...", tableName);
                String createTableQuery = String.format(
                        "CREATE TABLE %s (id SERIAL PRIMARY KEY, entry_date DATE NOT NULL UNIQUE, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)",
                        tableName
                );
                jdbcTemplate.execute(createTableQuery);
                log.info("Table created: {}", tableName);

                // Создаем индекс
                String createIndexQuery = String.format(
                        "CREATE INDEX idx_%s_date ON %s(entry_date)",
                        tableName, tableName
                );
                jdbcTemplate.execute(createIndexQuery);
            }

            // Проверяем существование колонки
            String checkColumnQuery = "SELECT EXISTS (SELECT FROM information_schema.columns WHERE table_name = ? AND column_name = ?)";
            Boolean columnExists = jdbcTemplate.queryForObject(checkColumnQuery, Boolean.class, tableName, columnName);
            log.info("Column exists: {}", columnExists);

            if (!columnExists) {
                log.warn("Column {} does not exist in table {}, creating...", columnName, tableName);
                String columnType = numericValue != null ? "INTEGER" : "VARCHAR(255)";
                String addColumnQuery = String.format("ALTER TABLE %s ADD COLUMN %s %s", tableName, columnName, columnType);
                jdbcTemplate.execute(addColumnQuery);
                log.info("Column created: {} ({})", columnName, columnType);
            }

            // Вставляем или обновляем данные
            int result;
            if (numericValue != null) {
                // Для числовых значений
                String upsertQuery = String.format(
                        "INSERT INTO %s (entry_date, %s) VALUES (CAST(? AS DATE), ?) ON CONFLICT (entry_date) DO UPDATE SET %s = ?, updated_at = CURRENT_TIMESTAMP",
                        tableName, columnName, columnName
                );

                result = jdbcTemplate.update(upsertQuery, dateStr, numericValue, numericValue);
                log.info("Upsert result: {} rows affected", result);
            } else {
                // Для строковых значений
                String upsertQuery = String.format(
                        "INSERT INTO %s (entry_date, %s) VALUES (CAST(? AS DATE), ?) ON CONFLICT (entry_date) DO UPDATE SET %s = ?, updated_at = CURRENT_TIMESTAMP",
                        tableName, columnName, columnName
                );

                result = jdbcTemplate.update(upsertQuery, dateStr, value, value);
                log.info("Upsert result: {} rows affected", result);
            }

            // Проверяем, что данные сохранились
            String verifyQuery = String.format("SELECT %s FROM %s WHERE entry_date = CAST(? AS DATE)", columnName, tableName);
            Object savedValue = jdbcTemplate.queryForObject(verifyQuery, Object.class, dateStr);
            log.info("Verified saved value: {}", savedValue);

            log.info("✅ Successfully saved to dynamic table for {}.{} on {}",
                    entry.getCategory(), entry.getPractice(), entry.getEntryDate());

        } catch (Exception e) {
            log.error("❌ Error in savePracticeValueToDynamicTable: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void saveYogaPractice(CalendarEntry entry) {
        log.info("=== SAVE YOGA PRACTICE ===");

        try {
            log.info("Saving yoga practice for date: {}", entry.getEntryDate());

            YogaPractice yogaPractice = yogaPracticeRepository
                    .findByEntryDate(entry.getEntryDate())
                    .orElse(new YogaPractice());

            yogaPractice.setEntryDate(entry.getEntryDate());

            // Парсим числовое значение из строки типа "30 мин"
            Integer value = parseDuration(entry.getDuration());
            log.info("Parsed duration: {} -> {}", entry.getDuration(), value);

            // Устанавливаем значения в зависимости от практики
            switch (entry.getPractice()) {
                case "Концентрация":
                    yogaPractice.setConcentrationMin(value);
                    log.info("Set concentration to {} min", value);
                    break;
                case "Аналитическая медитация":
                    yogaPractice.setAnalyticalMeditationMin(value);
                    log.info("Set analytical meditation to {} min", value);
                    break;
                case "Пранаяма":
                    yogaPractice.setPranayamaMin(value);
                    log.info("Set pranayama to {} min", value);
                    break;
                case "Экадаш":
                    yogaPractice.setEkadashCount(value);
                    log.info("Set ekadash to {} times", value);
                    break;
                case "Подъем":
                    log.info("Wake up time - not implemented yet");
                    break;
                case "Отбой":
                    log.info("Sleep time - not implemented yet");
                    break;
                default:
                    log.warn("Unknown yoga practice: {}", entry.getPractice());
            }

            yogaPracticeRepository.save(yogaPractice);
            log.info("✅ Successfully saved yoga practice for date: {}", entry.getEntryDate());

        } catch (Exception e) {
            log.error("❌ Error saving yoga practice: {}", e.getMessage(), e);
            throw e;
        }
    }

    private Integer parseDuration(String duration) {
        try {
            // Парсим строки типа "30 мин" в число
            return Integer.parseInt(duration.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            log.warn("Could not parse duration: {}", duration);
            return 0;
        }
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
        dto.setDuration(entry.getDuration());
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