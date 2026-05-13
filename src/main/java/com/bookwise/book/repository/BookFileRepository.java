package com.bookwise.book.repository;

import com.bookwise.book.entity.BookFile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookFileRepository extends JpaRepository<BookFile, Long> {

    List<BookFile> findByBookIdOrderByCreatedAtAsc(Long bookId);

    Optional<BookFile> findTopByBookIdOrderByCreatedAtAsc(Long bookId);

    Optional<BookFile> findByStorageKey(String storageKey);
}
