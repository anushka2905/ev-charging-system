package com.evcharging.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Payment entity — Phase 0 enhanced version.
 *
 * Changes from v1:
 *  - Added paymentMethod (UPI, CARD, WALLET, CASH)
 *  - Added transactionId for real payment gateway integration
 *  - Added paymentStatus enum (PENDING, SUCCESS, FAILED, REFUNDED)
 *  - Added currency field
 */
@Entity
@Table(name = "payments",
       indexes = {
           @Index(name = "idx_payment_booking", columnList = "booking_id"),
           @Index(name = "idx_payment_tx",      columnList = "transactionId", unique = false)
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Double amount;

    @Column(length = 10)
    @Builder.Default
    private String currency = "INR";

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime paymentTime = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private PaymentMethod paymentMethod = PaymentMethod.UPI;

    /** External payment gateway transaction reference */
    @Column(length = 100)
    private String transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    // ── Enums ────────────────────────────────────────────────────

    public enum PaymentStatus {
        PENDING, SUCCESS, FAILED, REFUNDED
    }

    public enum PaymentMethod {
        UPI, CARD, NET_BANKING, WALLET, CASH
    }
}
