package com.evcharging.service;

import java.util.List;

import com.evcharging.model.Payment;

public interface PaymentService {
    Payment processPayment(Payment payment);
    List<Payment> getAllPayments();
    Payment getPaymentById(Long id);
    void processPayment(Long bookingId, Double amount); // ← add this
}
