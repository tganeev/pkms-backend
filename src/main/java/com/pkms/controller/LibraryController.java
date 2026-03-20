package com.pkms.controller;

import com.pkms.dto.BookDTO;
import com.pkms.dto.LibraryViewDTO;
import com.pkms.dto.ReadingStatDTO;
import com.pkms.service.LibraryService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/library")
@RequiredArgsConstructor
public class LibraryController {

    private static final Logger log = LoggerFactory.getLogger(LibraryController.class);

    private final LibraryService libraryService;

    @GetMapping
    public ResponseEntity<LibraryViewDTO> getLibraryView(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("=== GET LIBRARY VIEW ===");
        log.info("Period: {} to {}", startDate, endDate);

        try {
            LibraryViewDTO view = libraryService.getLibraryView(startDate, endDate);
            return ResponseEntity.ok(view);
        } catch (Exception e) {
            log.error("Error getting library view: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/books")
    public ResponseEntity<List<BookDTO>> getAllBooks() {
        log.info("=== GET ALL BOOKS ===");
        try {
            LocalDate now = LocalDate.now();
            LibraryViewDTO view = libraryService.getLibraryView(now.minusMonths(1), now);
            return ResponseEntity.ok(view.getBooks());
        } catch (Exception e) {
            log.error("Error getting books: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/books")
    public ResponseEntity<BookDTO> createBook(@RequestBody BookDTO bookDTO) {
        log.info("=== CREATE BOOK ===");
        try {
            BookDTO created = libraryService.createBook(bookDTO);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            log.error("Error creating book: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/books/{id}")
    public ResponseEntity<BookDTO> updateBook(@PathVariable Long id, @RequestBody BookDTO bookDTO) {
        log.info("=== UPDATE BOOK ID: {} ===", id);
        try {
            BookDTO updated = libraryService.updateBook(id, bookDTO);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("Error updating book: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/books/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        log.info("=== DELETE BOOK ID: {} ===", id);
        try {
            libraryService.deleteBook(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error deleting book: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/books/{bookId}/stats")
    public ResponseEntity<ReadingStatDTO> addReadingStat(
            @PathVariable Long bookId,
            @RequestBody ReadingStatDTO statDTO) {
        log.info("=== ADD READING STAT for book ID: {}, date: {} ===", bookId, statDTO.getDate());
        try {
            ReadingStatDTO created = libraryService.addReadingStat(bookId, statDTO);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            log.error("Error adding reading stat: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/books/{bookId}/stats/{date}")
    public ResponseEntity<Void> deleteReadingStat(
            @PathVariable Long bookId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        log.info("=== DELETE READING STAT for book ID: {}, date: {} ===", bookId, date);
        try {
            libraryService.deleteReadingStat(bookId, date);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error deleting reading stat: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}