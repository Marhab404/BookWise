package com.bookwise.order.entity;

public enum OrderStatus {
    PENDING_PAYMENT,
    PAYMENT_PROOF_SUBMITTED,
    PAID,
    REJECTED,
    CANCELLED
}
