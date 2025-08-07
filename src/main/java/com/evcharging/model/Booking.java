package com.evcharging.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime bookingTime;

    @Enumerated(EnumType.STRING)
    private Status status;

    // Booking status values
    public enum Status {
        BOOKED,
        CANCELLED,
        COMPLETED,
        PAID
    }

    // Many bookings can belong to one slot
    @ManyToOne
    @JoinColumn(name = "slot_id")
    private ChargingSlot slot;

    // Many bookings can belong to one user
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    // --- Getters and Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getBookingTime() {
        return bookingTime;
    }

    public void setBookingTime(LocalDateTime bookingTime) {
        this.bookingTime = bookingTime;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public ChargingSlot getSlot() {
        return slot;
    }

    public void setSlot(ChargingSlot slot) {
        this.slot = slot;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
