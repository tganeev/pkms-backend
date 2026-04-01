package com.pkms.controller;

import com.pkms.dto.BookDTO;
import com.pkms.dto.LibraryViewDTO;
import com.pkms.dto.ReadingStatDTO;
import com.pkms.model.Book;
import com.pkms.model.ReadingStat;
import com.pkms.repository.BookRepository;
import com.pkms.repository.ReadingStatRepository;
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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/library")
@RequiredArgsConstructor
public class LibraryController {

    private static final Logger log = LoggerFactory.getLogger(LibraryController.class);

    private final LibraryService libraryService;
    private final BookRepository bookRepository;
    private final ReadingStatRepository readingStatRepository;

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
            List<Book> books = bookRepository.findAll();
            List<BookDTO> bookDTOs = books.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(bookDTOs);
        } catch (Exception e) {
            log.error("Error getting books: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/books/identifier/{identifier}")
    public ResponseEntity<BookDTO> getBookByIdentifier(@PathVariable String identifier) {
        log.info("=== GET BOOK BY IDENTIFIER: {} ===", identifier);
        try {
            Book book = bookRepository.findByIdentifier(identifier)
                    .orElseThrow(() -> new RuntimeException("Book not found"));
            return ResponseEntity.ok(convertToDTO(book));
        } catch (Exception e) {
            log.error("Error getting book by identifier: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
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

    private BookDTO convertToDTO(Book book) {
        BookDTO dto = new BookDTO();
        dto.setId(book.getId());
        dto.setTitle(book.getTitle());
        dto.setAuthor(book.getAuthor());
        dto.setStatus(book.getStatus());
        dto.setTotalPages(book.getTotalPages());
        dto.setLanguage(book.getLanguage());
        dto.setTotalPagesRead(book.getTotalPagesRead());
        dto.setTotalHoursRead(book.getTotalHoursRead());

        if (book.getCategory() != null) {
            dto.setCategoryId(book.getCategory().getId());
            dto.setCategoryName(book.getCategory().getName());
        }

        return dto;
    }
}