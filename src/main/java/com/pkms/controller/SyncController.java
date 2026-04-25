package com.pkms.controller;

import com.pkms.dto.*;
import com.pkms.model.Book;
import com.pkms.model.ReadingStat;
import com.pkms.model.ReadingStatId;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
public class SyncController {

    private static final Logger log = LoggerFactory.getLogger(SyncController.class);

    private final BookRepository bookRepository;
    private final ReadingStatRepository readingStatRepository;
    private final CategoryRepository categoryRepository;

    /**
     * Эндпоинт для отправки данных с мобильного устройства на сервер
     * POST /api/sync
     */
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
                    categoryRepository.findById(syncBook.getCategoryId())
                            .ifPresent(book::setCategory);
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
                        ReadingStat stat = readingStatRepository
                                .findById(new ReadingStatId(savedBook, syncStat.getDate()))
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

    /**
     * Эндпоинт для загрузки данных с сервера на мобильное устройство (история)
     * POST /api/sync/history
     */
    @PostMapping("/history")
    public ResponseEntity<SyncHistoryResponse> syncHistory(@RequestBody SyncHistoryRequest request) {
        log.info("=== SYNC HISTORY FROM SERVER ===");
        log.info("Username: {}", request.getUsername());
        log.info("Last sync timestamp: {}", request.getLastSyncTimestamp());

        SyncHistoryResponse response = new SyncHistoryResponse();

        try {
            SyncHistoryData data = new SyncHistoryData();
            List<SyncBookHistory> books = new ArrayList<>();
            List<SyncReadingStatHistory> stats = new ArrayList<>();

            // Получаем все книги из базы данных
            List<Book> allBooks = bookRepository.findAll();

            for (Book book : allBooks) {
                // Пропускаем книги без identifier
                if (book.getIdentifier() == null || book.getIdentifier().isEmpty()) {
                    log.warn("Skipping book without identifier: {}", book.getTitle());
                    continue;
                }

                // Конвертируем книгу в DTO для истории
                SyncBookHistory bookHistory = new SyncBookHistory();
                bookHistory.setServerId(book.getIdentifier());
                bookHistory.setTitle(book.getTitle());
                bookHistory.setAuthor(book.getAuthor());
                bookHistory.setTotalPages(book.getTotalPages());
                bookHistory.setCurrentPage(book.getTotalPagesRead());
                bookHistory.setReadingTime((long) (book.getTotalHoursRead() * 3600));
                bookHistory.setStatus(book.getStatus());

                if (book.getUpdatedAt() != null) {
                    bookHistory.setLastReadDate(book.getUpdatedAt().toEpochSecond(java.time.ZoneOffset.UTC) * 1000);
                }

                books.add(bookHistory);

                // Получаем статистику чтения для каждой книги
                List<ReadingStat> readingStats = readingStatRepository.findByBookId(book.getId());
                for (ReadingStat stat : readingStats) {
                    SyncReadingStatHistory statHistory = new SyncReadingStatHistory();
                    statHistory.setBookServerId(book.getIdentifier());
                    statHistory.setDate(stat.getDate().toString());
                    statHistory.setPagesRead(stat.getPagesRead());
                    statHistory.setHoursRead(stat.getHoursRead());
                    stats.add(statHistory);
                }
            }

            data.setBooks(books);
            data.setReadingStats(stats);
            data.setLastSyncTimestamp(System.currentTimeMillis());

            response.setSuccess(true);
            response.setData(data);

            log.info("History sync completed: {} books, {} stats", books.size(), stats.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error during history sync: {}", e.getMessage(), e);
            response.setSuccess(false);
            response.setError(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Тестовый эндпоинт для проверки работоспособности
     * GET /api/sync/test
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, String>> testSync() {
        log.info("=== TEST SYNC ENDPOINT ===");
        Map<String, String> response = new HashMap<>();
        response.put("status", "OK");
        response.put("message", "Sync endpoint is working");
        return ResponseEntity.ok(response);
    }
}