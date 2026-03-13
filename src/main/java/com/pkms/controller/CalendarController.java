package com.pkms.controller;

import com.pkms.dto.EntryDTO;
import com.pkms.dto.EntryStatusDTO;
import com.pkms.dto.WeekViewDTO;
import com.pkms.model.PracticeStandard;
import com.pkms.service.CalendarService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    private static final Logger log = LoggerFactory.getLogger(CalendarController.class);
    private final CalendarService calendarService;

    @GetMapping("/week")
    public ResponseEntity<?> getWeekEntries(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam String username) {

        log.info("=== GET WEEK ENTRIES CONTROLLER ===");
        log.info("Date: {}, Username: {}", date, username);

        try {
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
            return ResponseEntity.ok(weekView);

        } catch (Exception e) {
            log.error("Error in getWeekEntries: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> createEntry(@RequestBody EntryDTO entryDTO, @RequestParam String username) {
        log.info("=== CREATE ENTRY CONTROLLER ===");
        log.info("Username: {}", username);
        log.info("Entry: {}", entryDTO);

        try {
            EntryDTO created = calendarService.createEntry(entryDTO, username);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            log.error("Error in createEntry: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEntry(@PathVariable Long id, @RequestParam String username) {
        log.info("=== DELETE ENTRY CONTROLLER ===");
        log.info("ID: {}, Username: {}", id, username);

        try {
            calendarService.deleteEntry(id, username);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error in deleteEntry: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/status")
    public ResponseEntity<?> updateEntryStatus(@PathVariable Long id,
                                               @RequestBody EntryStatusDTO statusDTO,
                                               @RequestParam String username) {
        log.info("=== UPDATE ENTRY STATUS CONTROLLER ===");
        log.info("Entry ID: {}", id);
        log.info("Status: {}", statusDTO.getStatus());
        log.info("Username: {}", username);

        try {
            calendarService.updateEntryStatus(id, statusDTO, username);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error in updateEntryStatus: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/standards")
    public ResponseEntity<?> getStandards(@RequestParam String category) {
        log.info("=== GET STANDARDS CONTROLLER ===");
        log.info("Category: {}", category);

        try {
            List<PracticeStandard> standards = calendarService.getStandardsByCategory(category);
            return ResponseEntity.ok(standards);
        } catch (Exception e) {
            log.error("Error in getStandards: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/test")
    public String test() {
        log.info("✅ TEST ENDPOINT CALLED");
        return "CalendarController is working!";
    }
}