package com.pkms.service;

import com.pkms.dto.EntryDTO;
import com.pkms.dto.EntryStatusDTO;
import com.pkms.model.*;
import com.pkms.model.enums.Period;
import com.pkms.model.enums.RepeatInterval;
import com.pkms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CalendarService {

    private final CalendarRepository calendarRepository;
    private final UserRepository userRepository;
    private final EntryStatusRepository entryStatusRepository;
    private final PracticeStandardRepository practiceStandardRepository;
    private final YogaPracticeRepository yogaPracticeRepository;

    @Transactional
    public EntryDTO createEntry(EntryDTO entryDTO, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        CalendarEntry entry = new CalendarEntry();
        entry.setUser(user);
        entry.setCategory(entryDTO.getCategory());
        entry.setPractice(entryDTO.getPractice());
        entry.setDuration(entryDTO.getDuration());

        // Конвертация периода (оставляем как есть)
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

        // Теперь не нужно конвертировать вручную - конвертер сделает всё сам
        // Просто передаем строку, конвертер преобразует в enum
        entry.setRepeatInterval(RepeatInterval.fromDisplayName(entryDTO.getRepeatInterval()));

        entry.setEntryDate(entryDTO.getEntryDate());

        CalendarEntry savedEntry = calendarRepository.save(entry);
        return convertToDTO(savedEntry);
    }

    public List<EntryDTO> getWeekEntries(String username, LocalDate startDate, LocalDate endDate) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return calendarRepository
                .findByUserAndEntryDateBetweenOrderByEntryDateAscPeriodAsc(user, startDate, endDate)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteEntry(Long entryId, String username) {
        calendarRepository.deleteByUserIdAndId(
                userRepository.findByUsername(username).orElseThrow().getId(),
                entryId
        );
    }

    @Transactional
    public void updateEntryStatus(Long entryId, EntryStatusDTO statusDTO, String username) {
        CalendarEntry entry = calendarRepository.findById(entryId)
                .orElseThrow(() -> new RuntimeException("Entry not found"));

        // Проверяем, что запись принадлежит пользователю
        if (!entry.getUser().getUsername().equals(username)) {
            throw new RuntimeException("Unauthorized");
        }

        EntryStatus status = entry.getStatus();
        if (status == null) {
            status = new EntryStatus();
            status.setCalendarEntry(entry);
        }

        status.setStatus(statusDTO.getStatus());
        status.setNotes(statusDTO.getNotes());

        entryStatusRepository.save(status);

        // Если статус "completed" и это Yoga, записываем в yoga_practices
        if ("completed".equals(statusDTO.getStatus()) && "Yoga".equals(entry.getCategory())) {
            saveYogaPractice(entry);
        }
    }

    private void saveYogaPractice(CalendarEntry entry) {
        YogaPractice yogaPractice = yogaPracticeRepository
                .findByEntryDate(entry.getEntryDate())
                .orElse(new YogaPractice());

        yogaPractice.setEntryDate(entry.getEntryDate());

        // Устанавливаем значения в зависимости от практики
        switch (entry.getPractice()) {
            case "Концентрация":
                yogaPractice.setConcentrationMin(parseDuration(entry.getDuration()));
                break;
            case "Аналитическая медитация":
                yogaPractice.setAnalyticalMeditationMin(parseDuration(entry.getDuration()));
                break;
            case "Пранаяма":
                yogaPractice.setPranayamaMin(parseDuration(entry.getDuration()));
                break;
            case "Экадаш":
                yogaPractice.setEkadashCount(parseDuration(entry.getDuration()));
                break;
        }

        yogaPracticeRepository.save(yogaPractice);
    }

    private Integer parseDuration(String duration) {
        // Парсим строки типа "30 мин" в число
        return Integer.parseInt(duration.replaceAll("[^0-9]", ""));
    }

    public List<PracticeStandard> getStandardsByCategory(String categoryName) {
        Category category = null; // Нужно будет добавить CategoryRepository
        return practiceStandardRepository.findByCategoryId(category.getId());
    }

    private EntryDTO convertToDTO(CalendarEntry entry) {
        EntryDTO dto = new EntryDTO();
        dto.setId(entry.getId());
        dto.setCategory(entry.getCategory());
        dto.setPractice(entry.getPractice());
        dto.setDuration(entry.getDuration());

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