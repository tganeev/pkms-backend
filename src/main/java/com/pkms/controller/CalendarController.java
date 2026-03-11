package com.pkms.controller;

import com.pkms.dto.EntryDTO;
import com.pkms.dto.EntryStatusDTO;
import com.pkms.dto.WeekViewDTO;
import com.pkms.model.PracticeStandard;
import com.pkms.service.CalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService calendarService;

    @GetMapping("/week")
    public WeekViewDTO getWeekEntries(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam String username) {

        LocalDate startDate = date.with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1);
        LocalDate endDate = startDate.plusDays(6);

        List<EntryDTO> entries = calendarService.getWeekEntries(username, startDate, endDate);

        WeekViewDTO weekView = new WeekViewDTO();
        weekView.setStartDate(startDate);
        weekView.setEndDate(endDate);

        List<WeekViewDTO.DayDTO> days = startDate.datesUntil(endDate.plusDays(1))
                .map(day -> {
                    WeekViewDTO.DayDTO dayDTO = new WeekViewDTO.DayDTO();
                    dayDTO.setDate(day);
                    dayDTO.setDayName(day.getDayOfWeek().getDisplayName(
                            java.time.format.TextStyle.SHORT, Locale.forLanguageTag("ru")));

                    List<EntryDTO> dayEntries = entries.stream()
                            .filter(e -> e.getEntryDate().equals(day))
                            .collect(Collectors.toList());

                    dayDTO.setMorning(dayEntries.stream()
                            .filter(e -> "morning".equals(e.getPeriod()))
                            .collect(Collectors.toList()));
                    dayDTO.setDay(dayEntries.stream()
                            .filter(e -> "day".equals(e.getPeriod()))
                            .collect(Collectors.toList()));
                    dayDTO.setEvening(dayEntries.stream()
                            .filter(e -> "evening".equals(e.getPeriod()))
                            .collect(Collectors.toList()));

                    return dayDTO;
                })
                .collect(Collectors.toList());

        weekView.setDays(days);
        return weekView;
    }

    @PostMapping
    public EntryDTO createEntry(@RequestBody EntryDTO entryDTO, @RequestParam String username) {
        return calendarService.createEntry(entryDTO, username);
    }

    @DeleteMapping("/{id}")
    public void deleteEntry(@PathVariable Long id, @RequestParam String username) {
        calendarService.deleteEntry(id, username);
    }

    @PostMapping("/{id}/status")
    public void updateEntryStatus(@PathVariable Long id,
                                  @RequestBody EntryStatusDTO statusDTO,
                                  @RequestParam String username) {
        calendarService.updateEntryStatus(id, statusDTO, username);
    }

    @GetMapping("/standards")
    public List<PracticeStandard> getStandards(@RequestParam String category) {
        return calendarService.getStandardsByCategory(category);
    }
}