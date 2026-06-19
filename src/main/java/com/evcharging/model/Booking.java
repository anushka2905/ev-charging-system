package com.evcharging.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Booking entity — Phase 0 enhanced version.
 *
 * Changes from v1:
 *  - Added scheduledStartTime / scheduledEndTime (pre-booking support)
 *  - Added durationHours for NL booking (Phase 4)
 *  - Added cancellationReason (admin/user notes)
 *  - Added nlQuery — stores the original natural language query (Phase 4)
 *  - Added estimatedCost (calculated at booking time)
 *  - Audit fields: updatedAt
 */
@Entity
@Table(name = "bookings",
       indexes = {
           @Index(name = "idx_booking_user",   columnList = "user_id"),
           @Index(name = "idx_booking_slot",   columnList = "slot_id"),
           @Index(name = "idx_booking_status", columnList = "status"),
           @Index(name = "idx_booking_time",   columnList = "bookingTime")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime bookingTime = LocalDateTime.now();

    /** When the user plans to start charging */
    private LocalDateTime scheduledStartTime;

    /** When the user plans to stop charging */
    private LocalDateTime scheduledEndTime;

    /** Duration in hours (derived or explicitly set) */
    @Column
    @Builder.Default
    private double durationHours = 1.0;

    /** Estimated cost at booking time (pricePerUnit × powerKw × durationHours) */
    @Column
    private Double estimatedCost;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.BOOKED;

    /** Optional cancellation reason */
    @Column(length = 500)
    private String cancellationReason;

    /**
     * Phase 4: stores the original natural language query that triggered this booking.
     * Example: "Book a fast charger for tomorrow evening at Station XYZ"
     */
    @Column(length = 1000)
    private String nlQuery;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id", nullable = false)
    private ChargingSlot slot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column
    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ── Booking Status ────────────────────────────────────────────

    public enum Status {
        BOOKED,
        CONFIRMED,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED,
        PAID,
        REFUNDED
    }
}
