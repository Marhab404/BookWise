package com.bookwise.book.repository;

import com.bookwise.book.entity.Book;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<Book, Long> {

    @EntityGraph(attributePaths = {"authors", "categories"})
    List<Book> findByPublishedTrueOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"authors", "categories"})
    List<Book> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"authors", "categories", "files"})
    Optional<Book> findByIdAndPublishedTrue(Long id);

    @EntityGraph(attributePaths = {"authors", "categories", "files"})
    Optional<Book> findById(Long id);

    @Query("""
            select distinct b
            from Book b
            join b.files f
            where b.id = :bookId
            """)
    Optional<Book> findBookWithFiles(@Param("bookId") Long bookId);

    @Query("""
            select distinct b
            from com.bookwise.order.entity.OrderItem oi
            join oi.book b
            join oi.order o
            where o.user.id = :userId
              and o.status = com.bookwise.order.entity.OrderStatus.PAID
            order by b.createdAt desc
            """)
    List<Book> findPurchasedBooksByUserId(@Param("userId") Long userId);
}
