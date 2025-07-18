package com.ururulab.ururu.payment.domain.entity;

import com.ururulab.ururu.global.domain.entity.BaseEntity;
import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.order.domain.entity.Order;
import com.ururulab.ururu.payment.domain.entity.enumerated.PayMethod;
import com.ururulab.ururu.payment.domain.entity.enumerated.PaymentStatus;
import com.ururulab.ururu.payment.domain.policy.PaymentPolicy;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@Table(name = "payment")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(length = PaymentPolicy.PAYMENT_KEY_MAX_LENGTH)
    private String paymentKey;

    @Column(nullable = false)
    private Integer totalAmount;

    @Column(nullable = false)
    private Integer amount;

    @Column(nullable = false)
    private Integer point;

    @Enumerated(EnumType.STRING)
    @Column
    private PayMethod payMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column
    private Instant requestAt;

    @Column
    private Instant paidAt;

    @Column
    private Instant cancelledAt;

    @Version
    private Long version;

    public static Payment create(
            Member member,
            Order order,
            Integer totalAmount,
            Integer amount,
            Integer point
    ) {
        if (member == null) {
            throw new IllegalArgumentException(PaymentPolicy.MEMBER_REQUIRED);
        }
        if (order == null) {
            throw new IllegalArgumentException(PaymentPolicy.ORDER_REQUIRED);
        }
        if (totalAmount == null) {
            throw new IllegalArgumentException(PaymentPolicy.TOTAL_AMOUNT_REQUIRED);
        }
        if (totalAmount < PaymentPolicy.MIN_AMOUNT) {
            throw new IllegalArgumentException(PaymentPolicy.TOTAL_AMOUNT_MIN);
        }
        if (totalAmount > PaymentPolicy.MAX_AMOUNT) {
            throw new IllegalArgumentException(PaymentPolicy.TOTAL_AMOUNT_MAX);
        }

        if (amount == null) {
            throw new IllegalArgumentException(PaymentPolicy.AMOUNT_REQUIRED);
        }
        if (amount < PaymentPolicy.MIN_AMOUNT) {
            throw new IllegalArgumentException(PaymentPolicy.AMOUNT_MIN);
        }
        if (amount > PaymentPolicy.MAX_AMOUNT) {
            throw new IllegalArgumentException(PaymentPolicy.AMOUNT_MAX);
        }
        if (point == null) {
            throw new IllegalArgumentException(PaymentPolicy.POINT_REQUIRED);
        }
        if (point < PaymentPolicy.MIN_AMOUNT) {
            throw new IllegalArgumentException(PaymentPolicy.POINT_MIN);
        }
        if (point > PaymentPolicy.MAX_AMOUNT) {
            throw new IllegalArgumentException(PaymentPolicy.POINT_MAX);
        }
        if (!totalAmount.equals(amount + point)) {
            throw new IllegalArgumentException(PaymentPolicy.AMOUNT_MISMATCH);
        }

        Payment payment = new Payment();
        payment.member = member;
        payment.order = order;
        payment.totalAmount = totalAmount;
        payment.amount = amount;
        payment.point = point;
        payment.status = PaymentStatus.PENDING;
        payment.requestAt = Instant.now();

        return payment;
    }

    public void updatePaymentInfo(String paymentKey, PayMethod payMethod, Integer paidAmount) {
        if (this.status == PaymentStatus.PAID) {
            throw new IllegalStateException(PaymentPolicy.CANNOT_UPDATE_PAID);
        }
        if (this.status == PaymentStatus.PARTIAL_REFUNDED) {
            throw new IllegalStateException(PaymentPolicy.CANNOT_UPDATE_PARTIAL_REFUNDED);
        }
        if (this.status == PaymentStatus.REFUNDED) {
            throw new IllegalStateException(PaymentPolicy.CANNOT_UPDATE_REFUNDED);
        }
        if (paymentKey == null || paymentKey.trim().isEmpty()) {
            throw new IllegalArgumentException(PaymentPolicy.PAYMENT_KEY_REQUIRED);
        }
        if (payMethod == null) {
            throw new IllegalArgumentException(PaymentPolicy.PAY_METHOD_REQUIRED);
        }
        if (!this.amount.equals(paidAmount)) {
            throw new IllegalArgumentException(PaymentPolicy.PAYMENT_AMOUNT_MISMATCH);
        }

        this.paymentKey = paymentKey;
        this.payMethod = payMethod;
    }

    public void markAsPaid(Instant approvedAt) {
        if (approvedAt == null) {
            throw new IllegalArgumentException(PaymentPolicy.APPROVED_AT_REQUIRED);
        }
        if (this.status == PaymentStatus.PAID) {
            throw new IllegalStateException(PaymentPolicy.ALREADY_PAID);
        }
        if (this.status == PaymentStatus.PARTIAL_REFUNDED) {  // 추가 필요
            throw new IllegalStateException(PaymentPolicy.ALREADY_PARTIAL_REFUNDED);
        }
        if (this.status == PaymentStatus.REFUNDED) {
            throw new IllegalStateException(PaymentPolicy.ALREADY_REFUNDED);
        }

        this.status = PaymentStatus.PAID;
        this.paidAt = approvedAt;
    }

    public void markAsFailed() {
        if (this.status == PaymentStatus.PAID) {
            throw new IllegalStateException(PaymentPolicy.CANNOT_FAIL_PAID);
        }

        this.status = PaymentStatus.FAILED;
        this.cancelledAt = Instant.now();
    }

    public void markAsPartialRefunded() {
        if (this.status != PaymentStatus.PAID && this.status != PaymentStatus.PARTIAL_REFUNDED) {
            throw new IllegalStateException(PaymentPolicy.CANNOT_PARTIAL_REFUND_INVALID_STATUS);
        }
        this.status = PaymentStatus.PARTIAL_REFUNDED;
    }

    public void markAsRefunded(Instant cancelledAt) {
        if (cancelledAt == null) {
            throw new IllegalArgumentException(PaymentPolicy.CANCELLED_AT_REQUIRED);
        }
        if (this.status != PaymentStatus.PAID && this.status != PaymentStatus.PARTIAL_REFUNDED) {
            throw new IllegalStateException(PaymentPolicy.CANNOT_REFUND_INVALID_STATUS);
        }

        this.status = PaymentStatus.REFUNDED;
        this.cancelledAt = cancelledAt;
    }

    public boolean isPending() {return this.status == PaymentStatus.PENDING;}

    public boolean isPaid() {
        return this.status == PaymentStatus.PAID;
    }

    public boolean isRefundable() {
        return this.status == PaymentStatus.PAID || this.status == PaymentStatus.PARTIAL_REFUNDED;
    }

    public boolean isPartialRefunded() {
        return this.status == PaymentStatus.PARTIAL_REFUNDED;
    }

    public boolean isCancellable() {
        return this.status == PaymentStatus.PENDING;
    }
}