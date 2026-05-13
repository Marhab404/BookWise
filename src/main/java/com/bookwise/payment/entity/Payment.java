package com.bookwise.payment.entity;

import com.bookwise.common.AbstractAuditedEntity;
import com.bookwise.order.entity.PurchaseOrder;
import com.bookwise.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "payments",
        indexes = @Index(name = "idx_payments_status", columnList = "status")
)
public class Payment extends AbstractAuditedEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private PurchaseOrder order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentMethod method = PaymentMethod.BANK_TRANSFER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus status = PaymentStatus.WAITING_FOR_TRANSFER;

    @Column(length = 255)
    private String transferReference;

    @Column(length = 500)
    private String receiptFilePath;

    @Column(length = 255)
    private String receiptFileName;

    @Column(length = 100)
    private String receiptContentType;

    private Instant submittedAt;

    private Instant reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_admin_id")
    private User reviewedByAdmin;

    @Column(length = 2000)
    private String adminReviewNote;
}
