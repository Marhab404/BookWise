package com.bookwise.order.dto;

import com.bookwise.book.entity.Book;
import java.util.List;

public record CheckoutPreview(
        List<Book> books,
        long totalAmount,
        String currency
) {
}
