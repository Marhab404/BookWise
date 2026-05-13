package com.bookwise.order.service;

import com.bookwise.book.entity.Book;
import com.bookwise.book.repository.BookRepository;
import com.bookwise.common.MoneyUtils;
import com.bookwise.order.dto.CheckoutPreview;
import com.bookwise.order.entity.OrderItem;
import com.bookwise.order.entity.OrderStatus;
import com.bookwise.order.entity.PurchaseOrder;
import com.bookwise.order.repository.OrderRepository;
import com.bookwise.payment.entity.Payment;
import com.bookwise.payment.entity.PaymentMethod;
import com.bookwise.payment.entity.PaymentStatus;
import com.bookwise.payment.repository.PaymentRepository;
import com.bookwise.user.entity.User;
import com.bookwise.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class CheckoutService {

    private final BookRepository bookRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final MoneyUtils moneyUtils;

    public CheckoutService(
            BookRepository bookRepository,
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            UserRepository userRepository,
            MoneyUtils moneyUtils
    ) {
        this.bookRepository = bookRepository;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.moneyUtils = moneyUtils;
    }

    public CheckoutPreview previewCheckout(Long userId, List<Long> selectedBookIds) {
        List<Book> books = validateSelection(userId, selectedBookIds);
        long total = books.stream().mapToLong(Book::getPriceAmount).sum();
        String currency = books.isEmpty() ? "MAD" : books.get(0).getCurrency();
        return new CheckoutPreview(books, total, currency);
    }

    @Transactional
    public PurchaseOrder createOrder(Long userId, List<Long> selectedBookIds) {
        List<Book> books = validateSelection(userId, selectedBookIds);
        if (books.isEmpty()) {
            throw new IllegalArgumentException("Select at least one book");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        long total = books.stream().mapToLong(Book::getPriceAmount).sum();
        String currency = books.get(0).getCurrency().toUpperCase(Locale.ROOT);

        PurchaseOrder order = new PurchaseOrder();
        order.setUser(user);
        order.setTotalAmount(total);
        order.setCurrency(currency);
        order.setStatus(OrderStatus.PENDING_PAYMENT);

        List<OrderItem> items = new ArrayList<>();
        for (Book book : books) {
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setBook(book);
            item.setUnitPriceAmount(book.getPriceAmount());
            items.add(item);
        }
        order.setItems(items);
        order = orderRepository.save(order);

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setMethod(PaymentMethod.BANK_TRANSFER);
        payment.setStatus(PaymentStatus.WAITING_FOR_TRANSFER);
        paymentRepository.save(payment);

        order.setPayment(payment);
        return order;
    }

    private List<Book> validateSelection(Long userId, List<Long> selectedBookIds) {
        if (selectedBookIds == null || selectedBookIds.isEmpty()) {
            throw new IllegalArgumentException("Select at least one book");
        }

        List<Long> uniqueIds = selectedBookIds.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, Book> booksById = bookRepository.findAllById(uniqueIds).stream()
                .collect(Collectors.toMap(Book::getId, book -> book, (left, right) -> left, LinkedHashMap::new));

        if (booksById.size() != uniqueIds.size()) {
            throw new IllegalArgumentException("One or more books were not found");
        }

        List<Book> books = uniqueIds.stream().map(booksById::get).collect(Collectors.toList());
        for (Book book : books) {
            if (!book.isPublished()) {
                throw new IllegalArgumentException("You cannot purchase unpublished books");
            }
            boolean alreadyOwned = orderRepository.existsOwnedBook(userId, OrderStatus.PAID, book.getId());
            if (alreadyOwned) {
                throw new IllegalArgumentException("You already own one of the selected books");
            }
        }

        String currency = books.get(0).getCurrency();
        boolean mixedCurrency = books.stream().anyMatch(book -> !book.getCurrency().equalsIgnoreCase(currency));
        if (mixedCurrency) {
            throw new IllegalArgumentException("All selected books must use the same currency");
        }
        return books;
    }
}
