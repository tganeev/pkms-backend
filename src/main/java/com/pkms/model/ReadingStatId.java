package com.pkms.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDate;

@Data
@NoArgsConstructor
public class ReadingStatId implements Serializable {
    private Long book;
    private LocalDate date;

    public ReadingStatId(Long book, LocalDate date) {
        this.book = book;
        this.date = date;
    }

    public ReadingStatId(Book book, LocalDate date) {
        this.book = book.getId();
        this.date = date;
    }
}