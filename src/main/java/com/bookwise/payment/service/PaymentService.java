package com.bookwise.payment.service;

import com.bookwise.order.entity.OrderStatus;
import com.bookwise.order.entity.PurchaseOrder;
import com.bookwise.order.repository.OrderRepository;
import com.bookwise.payment.dto.PaymentProofSubmissionForm;
import com.bookwise.payment.entity.Payment;
import com.bookwise.payment.entity.PaymentStatus;
import com.bookwise.payment.repository.PaymentRepository;
import com.bookwise.user.entity.User;
import com.bookwise.user.repository.UserRepository;
import com.bookwise.storage.FileStorageService;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;

    public PaymentService(PaymentRepository paymentRepository, OrderRepository orderRepository, FileStorageService fileStorageService, UserRepository userRepository) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.fileStorageService = fileStorageService;
        this.userRepository = userRepository;
    }

    public List<Payment> listPendingReviews() {
        return paymentRepository.findByStatusOrderBySubmittedAtAsc(PaymentStatus.PROOF_SUBMITTED);
    }

    public Payment getForAdmin(Long paymentId) {
        return paymentRepository.findWithOrderAndUserById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
    }

    public Payment getForOrder(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
    }

    public Resource getProofResource(Long paymentId) {
        Payment payment = getForAdmin(paymentId);
        if (payment.getReceiptFilePath() == null || payment.getReceiptFilePath().isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment proof not found");
        }
        return fileStorageService.loadAsResource(payment.getReceiptFilePath());
    }

    @Transactional
    public Payment submitProof(Long userId, Long orderId, PaymentProofSubmissionForm form) {
        PurchaseOrder order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT && order.getStatus() != OrderStatus.REJECTED) {
            throw new IllegalArgumentException("This order cannot accept a payment proof right now");
        }

        Payment payment = getForOrder(orderId);
        String previousStorageKey = payment.getReceiptFilePath();
        MultipartFile receiptFile = form.getReceiptFile();
        var stored = fileStorageService.storePaymentProof(orderId, receiptFile);
        payment.setReceiptFilePath(stored.storageKey());
        payment.setReceiptFileName(stored.originalFileName());
        payment.setReceiptContentType(stored.contentType());
        payment.setTransferReference(trimToNull(form.getTransferReference()));
        payment.setStatus(PaymentStatus.PROOF_SUBMITTED);
        payment.setSubmittedAt(Instant.now());
        payment.setReviewedAt(null);
        payment.setReviewedByAdmin(null);
        payment.setAdminReviewNote(null);
        order.setStatus(OrderStatus.PAYMENT_PROOF_SUBMITTED);
        Payment saved = paymentRepository.save(payment);
        orderRepository.save(order);
        if (previousStorageKey != null && !previousStorageKey.isBlank()) {
            fileStorageService.deleteIfExists(previousStorageKey);
        }
        return saved;
    }

    @Transactional
    public Payment approve(Long paymentId, Long adminUserId, String adminReviewNote) {
        Payment payment = getForAdmin(paymentId);
        if (payment.getStatus() != PaymentStatus.PROOF_SUBMITTED) {
            throw new IllegalArgumentException("Only submitted proofs can be approved");
        }
        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new IllegalArgumentException("Admin user not found"));
        payment.setStatus(PaymentStatus.APPROVED);
        payment.setReviewedAt(Instant.now());
        payment.setAdminReviewNote(trimToNull(adminReviewNote));
        payment.getOrder().setStatus(OrderStatus.PAID);
        payment.setReviewedByAdmin(admin);
        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment reject(Long paymentId, Long adminUserId, String adminReviewNote) {
        Payment payment = getForAdmin(paymentId);
        if (payment.getStatus() != PaymentStatus.PROOF_SUBMITTED) {
            throw new IllegalArgumentException("Only submitted proofs can be rejected");
        }
        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new IllegalArgumentException("Admin user not found"));
        payment.setStatus(PaymentStatus.REJECTED);
        payment.setReviewedAt(Instant.now());
        payment.setAdminReviewNote(trimToNull(adminReviewNote));
        payment.getOrder().setStatus(OrderStatus.REJECTED);
        payment.setReviewedByAdmin(admin);
        return paymentRepository.save(payment);
    }

    private String trimToNull(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
