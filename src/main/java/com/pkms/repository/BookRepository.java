package com.pkms.repository;

import com.pkms.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    List<Book> findByCategoryId(Long categoryId);
    List<Book> findByStatus(String status);

    @Query("SELECT b FROM Book b ORDER BY b.id")
    List<Book> findAllOrdered();
}