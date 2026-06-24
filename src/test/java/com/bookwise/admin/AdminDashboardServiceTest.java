package com.bookwise.admin;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private BookRepository bookRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private PaymentRepository paymentRepository;

    private AdminDashboardService service;

    @BeforeEach
    void setUp() {
        service = new AdminDashboardService(userRepository, bookRepository, orderRepository, paymentRepository);
    }

    @Test
    void prepareKpiCards_shouldReturnAllCards() {
        when(userRepository.count()).thenReturn(10L);
        when(userRepository.countByRole(UserRole.ADMIN)).thenReturn(2L);
        when(bookRepository.count()).thenReturn(20L);
        when(bookRepository.countByPublishedTrue()).thenReturn(15L);
        when(orderRepository.count()).thenReturn(30L);
        when(orderRepository.countByStatus(OrderStatus.PAID)).thenReturn(12L);
        when(paymentRepository.countByStatus(PaymentStatus.WAITING_FOR_TRANSFER)).thenReturn(3L);
        when(paymentRepository.countByStatus(PaymentStatus.PROOF_SUBMITTED)).thenReturn(2L);

        List<KpiCard> cards = service.prepareKpiCards();

        assertEquals(8, cards.size());
        assertEquals("Total users", cards.get(0).label());
        assertEquals(10, cards.get(0).value());
        assertEquals("Pending payments", cards.get(7).label());
        assertEquals(5, cards.get(7).value());
    }

    @Test
    void prepareKpiCards_shouldHandleZeroCounts() {
        when(userRepository.count()).thenReturn(0L);
        when(userRepository.countByRole(UserRole.ADMIN)).thenReturn(0L);
        when(bookRepository.count()).thenReturn(0L);
        when(bookRepository.countByPublishedTrue()).thenReturn(0L);
        when(orderRepository.count()).thenReturn(0L);
        when(orderRepository.countByStatus(OrderStatus.PAID)).thenReturn(0L);
        when(paymentRepository.countByStatus(PaymentStatus.WAITING_FOR_TRANSFER)).thenReturn(0L);
        when(paymentRepository.countByStatus(PaymentStatus.PROOF_SUBMITTED)).thenReturn(0L);

        List<KpiCard> cards = service.prepareKpiCards();

        cards.forEach(card -> assertEquals(0, card.value()));
    }

    @Test
    void prepareUsersByRole_shouldReturnTwoSlices() {
        when(userRepository.countByRole(UserRole.ADMIN)).thenReturn(3L);
        when(userRepository.countByRole(UserRole.READER)).thenReturn(7L);

        List<PieSlice> slices = service.prepareUsersByRole();

        assertEquals(2, slices.size());
        assertEquals("Admins", slices.get(0).label());
        assertEquals(3, slices.get(0).value());
        assertEquals("Readers", slices.get(1).label());
        assertEquals(7, slices.get(1).value());
    }

    @Test
    void prepareOrdersByStatus_shouldIncludeOnlyNonZeroStatuses() {
        when(orderRepository.countByStatus(OrderStatus.PENDING_PAYMENT)).thenReturn(5L);
        when(orderRepository.countByStatus(OrderStatus.PAYMENT_PROOF_SUBMITTED)).thenReturn(0L);
        when(orderRepository.countByStatus(OrderStatus.PAID)).thenReturn(10L);
        when(orderRepository.countByStatus(OrderStatus.REJECTED)).thenReturn(2L);
        when(orderRepository.countByStatus(OrderStatus.CANCELLED)).thenReturn(0L);

        List<PieSlice> slices = service.prepareOrdersByStatus();

        assertEquals(3, slices.size());
    }

    @Test
    void preparePaymentsByStatus_shouldIncludeOnlyNonZeroStatuses() {
        when(paymentRepository.countByStatus(PaymentStatus.WAITING_FOR_TRANSFER)).thenReturn(3L);
        when(paymentRepository.countByStatus(PaymentStatus.PROOF_SUBMITTED)).thenReturn(0L);
        when(paymentRepository.countByStatus(PaymentStatus.APPROVED)).thenReturn(10L);
        when(paymentRepository.countByStatus(PaymentStatus.REJECTED)).thenReturn(1L);

        List<PieSlice> slices = service.preparePaymentsByStatus();

        assertEquals(3, slices.size());
    }

    @Test
    void prepareMonthlyRevenue_shouldReturnSixMonths() {
        PurchaseOrder order = new PurchaseOrder();
        order.setTotalAmount(5000L);
        order.setCreatedAt(ZonedDateTime.now(ZoneId.of("Africa/Casablanca")).minusMonths(1).toInstant());

        when(orderRepository.findByStatusAndCreatedAtAfter(eq(OrderStatus.PAID), any()))
                .thenReturn(List.of(order));

        BarSeries series = service.prepareMonthlyRevenue();

        assertEquals("Revenue (MAD)", series.label());
        assertEquals(6, series.labels().size());
        assertEquals(6, series.values().size());
        assertTrue(series.values().stream().anyMatch(v -> v > 0));
    }

    @Test
    void prepareMonthlyRevenue_shouldHandleEmptyData() {
        when(orderRepository.findByStatusAndCreatedAtAfter(eq(OrderStatus.PAID), any()))
                .thenReturn(List.of());

        BarSeries series = service.prepareMonthlyRevenue();

        assertEquals(6, series.values().size());
        series.values().forEach(v -> assertEquals(0L, v));
    }

    @Test
    void prepareTopSellingBooks_shouldReturnLimitedResults() {
        List<Object[]> rawData = List.of(
                new Object[]{1L, "Book A", 10L},
                new Object[]{2L, "Book B", 8L},
                new Object[]{3L, "Book C", 5L}
        );
        when(orderRepository.findTopSellingBooks(OrderStatus.PAID)).thenReturn(rawData);

        List<TopBook> topBooks = service.prepareTopSellingBooks();

        assertEquals(3, topBooks.size());
        assertEquals("Book A", topBooks.get(0).title());
        assertEquals(10, topBooks.get(0).sold());
    }

    @Test
    void prepareTopSellingBooks_shouldHandleEmpty() {
        when(orderRepository.findTopSellingBooks(OrderStatus.PAID)).thenReturn(List.of());

        List<TopBook> topBooks = service.prepareTopSellingBooks();

        assertTrue(topBooks.isEmpty());
    }

    @Test
    void prepareMonthlyOrderCount_shouldReturnSixMonths() {
        PurchaseOrder order = new PurchaseOrder();
        order.setTotalAmount(3000L);
        order.setCreatedAt(ZonedDateTime.now(ZoneId.of("Africa/Casablanca")).minusMonths(2).toInstant());

        when(orderRepository.findByStatusAndCreatedAtAfter(eq(OrderStatus.PAID), any()))
                .thenReturn(List.of(order));

        BarSeries series = service.prepareMonthlyOrderCount();

        assertEquals("Orders", series.label());
        assertEquals(6, series.labels().size());
        assertEquals(6, series.values().size());
        assertTrue(series.values().stream().anyMatch(v -> v > 0));
    }
}
