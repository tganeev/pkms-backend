package com.pkms.controller;

import com.pkms.dto.SyncRequestDTO;
import com.pkms.dto.SyncBookDTO;
import com.pkms.dto.SyncReadingStatDTO;
import com.pkms.model.Book;
import com.pkms.model.ReadingStat;
import com.pkms.model.ReadingStatId;
import com.pkms.model.Category;
import com.pkms.repository.BookRepository;
import com.pkms.repository.ReadingStatRepository;
import com.pkms.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
public class SyncController {

    private static final Logger log = LoggerFactory.getLogger(SyncController.class);

    private final BookRepository bookRepository;
    private final ReadingStatRepository readingStatRepository;
    private final CategoryRepository categoryRepository;

    @PostMapping
    @Transactional
    public ResponseEntity<?> syncData(@RequestBody SyncRequestDTO syncRequest) {
        log.info("=== SYNC DATA FROM MOBILE ===");
        log.info("Username: {}", syncRequest.getUsername());
        log.info("Books to sync: {}", syncRequest.getBooks().size());

        Map<String, Object> response = new HashMap<>();
        int booksCreated = 0;
        int booksUpdated = 0;
        int statsCreated = 0;
        int statsUpdated = 0;

        try {
            for (SyncBookDTO syncBook : syncRequest.getBooks()) {
                log.info("Processing book: {} (identifier: {})", syncBook.getTitle(), syncBook.getIdentifier());

                // Ищем книгу по identifier
                Book book = bookRepository.findByIdentifier(syncBook.getIdentifier())
                        .orElse(new Book());

                boolean isNewBook = (book.getId() == null);

                // Заполняем данные книги
                book.setTitle(syncBook.getTitle());
                book.setAuthor(syncBook.getAuthor());
                book.setIdentifier(syncBook.getIdentifier());
                book.setTotalPages(syncBook.getTotalPages());
                book.setLanguage(syncBook.getLanguage());

                // Устанавливаем категорию, если указана
                if (syncBook.getCategoryId() != null) {
                    Category category = categoryRepository.findById(syncBook.getCategoryId())
                            .orElse(null);
                    book.setCategory(category);
                }

                // Устанавливаем статус по умолчанию, если книга новая
                if (isNewBook) {
                    book.setStatus("In process");
                    book.setTotalPagesRead(0);
                    book.setTotalHoursRead(0.0);
                }

                Book savedBook = bookRepository.save(book);

                if (isNewBook) {
                    booksCreated++;
                    log.info("Created new book with ID: {}", savedBook.getId());
                } else {
                    booksUpdated++;
                    log.info("Updated existing book with ID: {}", savedBook.getId());
                }

                // Обрабатываем ежедневную статистику
                if (syncBook.getReadingStats() != null && !syncBook.getReadingStats().isEmpty()) {
                    int totalPagesFromStats = 0;
                    double totalHoursFromStats = 0.0;

                    for (SyncReadingStatDTO syncStat : syncBook.getReadingStats()) {
                        ReadingStatId statId = new ReadingStatId(savedBook, syncStat.getDate());
                        ReadingStat stat = readingStatRepository.findById(statId)
                                .orElse(new ReadingStat());

                        boolean isNewStat = (stat.getBook() == null);

                        stat.setBook(savedBook);
                        stat.setDate(syncStat.getDate());
                        stat.setPagesRead(syncStat.getPagesRead());
                        stat.setHoursRead(syncStat.getHoursRead());

                        readingStatRepository.save(stat);

                        if (isNewStat) {
                            statsCreated++;
                        } else {
                            statsUpdated++;
                        }

                        // Суммируем для обновления общих значений книги
                        totalPagesFromStats += syncStat.getPagesRead();
                        totalHoursFromStats += syncStat.getHoursRead();
                    }

                    // Обновляем агрегированные значения в книге
                    savedBook.setTotalPagesRead(totalPagesFromStats);
                    savedBook.setTotalHoursRead(totalHoursFromStats);
                    bookRepository.save(savedBook);

                    log.info("Updated book totals: pages={}, hours={}",
                            totalPagesFromStats, totalHoursFromStats);
                }
            }

            response.put("success", true);
            response.put("booksCreated", booksCreated);
            response.put("booksUpdated", booksUpdated);
            response.put("statsCreated", statsCreated);
            response.put("statsUpdated", statsUpdated);
            response.put("message", "Sync completed successfully");

            log.info("Sync completed: books created={}, updated={}, stats created={}, updated={}",
                    booksCreated, booksUpdated, statsCreated, statsUpdated);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error during sync: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/test")
    public ResponseEntity<?> testSync() {
        log.info("=== TEST SYNC ENDPOINT ===");
        Map<String, String> response = new HashMap<>();
        response.put("status", "OK");
        response.put("message", "Sync endpoint is working");
        return ResponseEntity.ok(response);
    }
}