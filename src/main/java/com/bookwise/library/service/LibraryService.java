package com.bookwise.library.service;

import com.bookwise.book.entity.Book;
import com.bookwise.book.entity.BookFile;
import com.bookwise.book.repository.BookFileRepository;
import com.bookwise.book.repository.BookRepository;
import com.bookwise.order.entity.OrderStatus;
import com.bookwise.order.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LibraryService {

    private final BookRepository bookRepository;
    private final BookFileRepository bookFileRepository;
    private final OrderRepository orderRepository;

    public LibraryService(BookRepository bookRepository, BookFileRepository bookFileRepository, OrderRepository orderRepository) {
        this.bookRepository = bookRepository;
        this.bookFileRepository = bookFileRepository;
        this.orderRepository = orderRepository;
    }

    public java.util.List<Book> purchasedBooks(Long userId) {
        return bookRepository.findPurchasedBooksByUserId(userId);
    }

    public BookFile firstOwnedBookFile(Long userId, Long bookId) {
        boolean owns = orderRepository.existsOwnedBook(userId, OrderStatus.PAID, bookId);
        if (!owns) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this book");
        }
        return bookFileRepository.findTopByBookIdOrderByCreatedAtAsc(bookId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "This book has no digital file"));
    }
}
