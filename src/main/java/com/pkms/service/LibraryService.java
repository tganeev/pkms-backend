package com.pkms.service;

import com.pkms.dto.BookDTO;
import com.pkms.dto.LibraryViewDTO;
import com.pkms.dto.ReadingStatDTO;
import com.pkms.model.Book;
import com.pkms.model.ReadingStatId;
import com.pkms.model.ReadingStat;
import com.pkms.model.Category;
import com.pkms.repository.BookRepository;
import com.pkms.repository.ReadingStatRepository;
import com.pkms.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LibraryService {

    private static final Logger log = LoggerFactory.getLogger(LibraryService.class);

    private final BookRepository bookRepository;
    private final ReadingStatRepository readingStatRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public BookDTO createBook(BookDTO bookDTO) {
        log.info("Creating book: {}", bookDTO.getTitle());

        Book book = new Book();
        book.setTitle(bookDTO.getTitle());
        book.setAuthor(bookDTO.getAuthor());
        book.setStatus(bookDTO.getStatus());
        book.setTotalPages(bookDTO.getTotalPages());
        book.setLanguage(bookDTO.getLanguage());
        book.setTotalPagesRead(0);
        book.setTotalHoursRead(0.0);

        if (bookDTO.getCategoryId() != null) {
            Category category = categoryRepository.findById(bookDTO.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
            book.setCategory(category);
        }

        Book savedBook = bookRepository.save(book);
        return convertToDTO(savedBook);
    }

    @Transactional
    public BookDTO updateBook(Long id, BookDTO bookDTO) {
        log.info("Updating book ID: {}", id);

        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Book not found"));

        book.setTitle(bookDTO.getTitle());
        book.setAuthor(bookDTO.getAuthor());
        book.setStatus(bookDTO.getStatus());
        book.setTotalPages(bookDTO.getTotalPages());
        book.setLanguage(bookDTO.getLanguage());

        if (bookDTO.getCategoryId() != null) {
            Category category = categoryRepository.findById(bookDTO.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
            book.setCategory(category);
        } else {
            book.setCategory(null);
        }

        Book updatedBook = bookRepository.save(book);
        return convertToDTO(updatedBook);
    }

    @Transactional
    public void deleteBook(Long id) {
        log.info("Deleting book ID: {}", id);
        bookRepository.deleteById(id);
    }

    @Transactional
    public ReadingStatDTO addReadingStat(Long bookId, ReadingStatDTO statDTO) {
        log.info("Adding reading stat for book ID: {}, date: {}", bookId, statDTO.getDate());

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Book not found"));

        ReadingStat stat = readingStatRepository.findById(new ReadingStatId(book, statDTO.getDate()))
                .orElse(new ReadingStat());

        stat.setBook(book);
        stat.setDate(statDTO.getDate());
        stat.setPagesRead(statDTO.getPagesRead());
        stat.setHoursRead(statDTO.getHoursRead());

        ReadingStat savedStat = readingStatRepository.save(stat);

        // Обновляем тоталы в книге
        updateBookTotals(book);

        return convertToDTO(savedStat);
    }

    @Transactional
    public void deleteReadingStat(Long bookId, LocalDate date) {
        log.info("Deleting reading stat for book ID: {}, date: {}", bookId, date);

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Book not found"));

        readingStatRepository.deleteById(new ReadingStatId(book, date));

        // Обновляем тоталы в книге
        updateBookTotals(book);
    }

    private void updateBookTotals(Book book) {
        List<ReadingStat> stats = readingStatRepository.findByBookId(book.getId());

        int totalPages = stats.stream().mapToInt(ReadingStat::getPagesRead).sum();
        double totalHours = stats.stream().mapToDouble(ReadingStat::getHoursRead).sum();

        book.setTotalPagesRead(totalPages);
        book.setTotalHoursRead(totalHours);

        bookRepository.save(book);
    }

    public LibraryViewDTO getLibraryView(LocalDate startDate, LocalDate endDate) {
        log.info("Getting library view from {} to {}", startDate, endDate);

        List<Book> books = bookRepository.findAllOrdered();
        List<Long> bookIds = books.stream().map(Book::getId).collect(Collectors.toList());

        List<ReadingStat> stats = readingStatRepository.findByBooksAndDateRange(bookIds, startDate, endDate);

        // Группируем статистику по книгам и датам
        Map<Long, Map<LocalDate, ReadingStatDTO>> statsMap = new LinkedHashMap<>();

        for (ReadingStat stat : stats) {
            statsMap
                    .computeIfAbsent(stat.getBook().getId(), k -> new LinkedHashMap<>())
                    .put(stat.getDate(), convertToDTO(stat));
        }

        // Генерируем список дат за период
        List<LocalDate> dates = new ArrayList<>();
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            dates.add(currentDate);
            currentDate = currentDate.plusDays(1);
        }

        // Считаем тоталы за период
        int totalPagesAll = stats.stream().mapToInt(ReadingStat::getPagesRead).sum();
        double totalHoursAll = stats.stream().mapToDouble(ReadingStat::getHoursRead).sum();

        LibraryViewDTO view = new LibraryViewDTO();
        view.setBooks(books.stream().map(this::convertToDTO).collect(Collectors.toList()));
        view.setDates(dates);
        view.setStats(statsMap);
        view.setStartDate(startDate);
        view.setEndDate(endDate);
        view.setTotalPagesAll(totalPagesAll);
        view.setTotalHoursAll(totalHoursAll);

        return view;
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

    private ReadingStatDTO convertToDTO(ReadingStat stat) {
        ReadingStatDTO dto = new ReadingStatDTO();
        dto.setBookId(stat.getBook().getId());
        dto.setDate(stat.getDate());
        dto.setPagesRead(stat.getPagesRead());
        dto.setHoursRead(stat.getHoursRead());
        return dto;
    }
}