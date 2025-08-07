package com.evcharging.repository;

import com.evcharging.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    // Optional: Add methods like findByBookingId if needed
}
