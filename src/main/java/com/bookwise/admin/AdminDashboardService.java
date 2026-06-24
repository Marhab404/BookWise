package com.bookwise.admin;

import com.bookwise.admin.dto.BarSeries;
import com.bookwise.admin.dto.KpiCard;
import com.bookwise.admin.dto.PieSlice;
import com.bookwise.admin.dto.TopBook;
import com.bookwise.book.repository.BookRepository;
import com.bookwise.order.entity.OrderStatus;
import com.bookwise.order.entity.PurchaseOrder;
import com.bookwise.order.repository.OrderRepository;
import com.bookwise.payment.entity.PaymentStatus;
import com.bookwise.payment.repository.PaymentRepository;
import com.bookwise.user.entity.UserRole;
import com.bookwise.user.repository.UserRepository;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class AdminDashboardService {

    private static final ZoneId ZONE = ZoneId.of("Africa/Casablanca");
    private static final String[] PIE_COLORS = {
            "#14b8a6", "#0f766e", "#115e59", "#134e4a", "#042f2e",
            "#ecfeff", "#cffafe", "#a5f3fc", "#5eead4", "#2dd4bf"
    };
    private static final String[] BAR_COLORS = {
            "#14b8a6", "#0ea5e9", "#f59e0b", "#ef4444", "#8b5cf6"
    };

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    public AdminDashboardService(UserRepository userRepository, BookRepository bookRepository,
                                 OrderRepository orderRepository, PaymentRepository paymentRepository) {
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
    }

    public List<KpiCard> prepareKpiCards() {
        long totalUsers = userRepository.count();
        long adminUsers = userRepository.countByRole(UserRole.ADMIN);
        long readerUsers = totalUsers - adminUsers;
        long totalBooks = bookRepository.count();
        long publishedBooks = bookRepository.countByPublishedTrue();
        long totalOrders = orderRepository.count();
        long paidOrders = orderRepository.countByStatus(OrderStatus.PAID);
        long pendingPayments = paymentRepository.countByStatus(PaymentStatus.WAITING_FOR_TRANSFER)
                + paymentRepository.countByStatus(PaymentStatus.PROOF_SUBMITTED);

        return List.of(
                new KpiCard("Total users", totalUsers, "including admins & readers"),
                new KpiCard("Admins", adminUsers, "administrators"),
                new KpiCard("Readers", readerUsers, "registered readers"),
                new KpiCard("Books", totalBooks, "total in catalog"),
                new KpiCard("Published books", publishedBooks, "publicly visible"),
                new KpiCard("Orders", totalOrders, "all placed orders"),
                new KpiCard("Paid orders", paidOrders, "completed sales"),
                new KpiCard("Pending payments", pendingPayments, "awaiting transfer or proof")
        );
    }

    public List<PieSlice> prepareUsersByRole() {
        long admins = userRepository.countByRole(UserRole.ADMIN);
        long readers = userRepository.countByRole(UserRole.READER);
        return List.of(
                new PieSlice("Admins", admins, PIE_COLORS[0]),
                new PieSlice("Readers", readers, PIE_COLORS[1])
        );
    }

    public List<PieSlice> prepareOrdersByStatus() {
        List<PieSlice> slices = new ArrayList<>();
        int i = 0;
        for (OrderStatus status : OrderStatus.values()) {
            long count = orderRepository.countByStatus(status);
            if (count > 0) {
                slices.add(new PieSlice(label(status), count, PIE_COLORS[i % PIE_COLORS.length]));
                i++;
            }
        }
        return slices;
    }

    public List<PieSlice> preparePaymentsByStatus() {
        List<PieSlice> slices = new ArrayList<>();
        int i = 0;
        for (PaymentStatus status : PaymentStatus.values()) {
            long count = paymentRepository.countByStatus(status);
            if (count > 0) {
                slices.add(new PieSlice(label(status), count, PIE_COLORS[i % PIE_COLORS.length]));
                i++;
            }
        }
        return slices;
    }

    public BarSeries prepareMonthlyRevenue() {
        ZonedDateTime now = ZonedDateTime.now(ZONE);
        Instant sixMonthsAgo = now.minusMonths(6).toInstant();
        List<PurchaseOrder> paidOrders = orderRepository.findByStatusAndCreatedAtAfter(
                OrderStatus.PAID, sixMonthsAgo);

        Map<String, Long> revenueByMonth = new HashMap<>();
        for (PurchaseOrder order : paidOrders) {
            String monthKey = monthKey(order.getCreatedAt());
            revenueByMonth.merge(monthKey, order.getTotalAmount(), Long::sum);
        }

        List<String> labels = new ArrayList<>();
        List<Long> values = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            ZonedDateTime monthStart = now.minusMonths(i);
            String key = monthKey(monthStart.toInstant());
            labels.add(monthLabel(monthStart.toInstant()));
            values.add(revenueByMonth.getOrDefault(key, 0L));
        }

        return new BarSeries("Revenue (MAD)", labels, values);
    }

    public BarSeries prepareMonthlyOrderCount() {
        ZonedDateTime now = ZonedDateTime.now(ZONE);
        Instant sixMonthsAgo = now.minusMonths(6).toInstant();
        List<PurchaseOrder> paidOrders = orderRepository.findByStatusAndCreatedAtAfter(
                OrderStatus.PAID, sixMonthsAgo);

        Map<String, Long> countByMonth = new HashMap<>();
        for (PurchaseOrder order : paidOrders) {
            String monthKey = monthKey(order.getCreatedAt());
            countByMonth.merge(monthKey, 1L, Long::sum);
        }

        List<String> labels = new ArrayList<>();
        List<Long> values = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            ZonedDateTime monthStart = now.minusMonths(i);
            String key = monthKey(monthStart.toInstant());
            labels.add(monthLabel(monthStart.toInstant()));
            values.add(countByMonth.getOrDefault(key, 0L));
        }

        return new BarSeries("Orders", labels, values);
    }

    public List<TopBook> prepareTopSellingBooks() {
        List<Object[]> raw = orderRepository.findTopSellingBooks(OrderStatus.PAID);
        return raw.stream()
                .limit(10)
                .map(row -> new TopBook((String) row[1], (Long) row[2]))
                .collect(Collectors.toList());
    }

    private String monthKey(Instant instant) {
        var zdt = instant.atZone(ZONE);
        return zdt.getYear() + "-" + String.format("%02d", zdt.getMonthValue());
    }

    private String monthLabel(Instant instant) {
        var zdt = instant.atZone(ZONE);
        return zdt.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + zdt.getYear();
    }

    private String label(OrderStatus status) {
        return switch (status) {
            case PENDING_PAYMENT -> "Pending payment";
            case PAYMENT_PROOF_SUBMITTED -> "Proof submitted";
            case PAID -> "Paid";
            case REJECTED -> "Rejected";
            case CANCELLED -> "Cancelled";
        };
    }

    private String label(PaymentStatus status) {
        return switch (status) {
            case WAITING_FOR_TRANSFER -> "Waiting for transfer";
            case PROOF_SUBMITTED -> "Proof submitted";
            case APPROVED -> "Approved";
            case REJECTED -> "Rejected";
        };
    }
}
