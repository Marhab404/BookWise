package com.bookwise.payment.repository;

import com.bookwise.payment.entity.Payment;
import com.bookwise.payment.entity.PaymentStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(Long orderId);

    List<Payment> findByStatusOrderBySubmittedAtAsc(PaymentStatus status);

    @EntityGraph(attributePaths = {"order", "order.items", "order.items.book", "order.user", "reviewedByAdmin"})
    Optional<Payment> findWithOrderAndUserById(Long id);
}
