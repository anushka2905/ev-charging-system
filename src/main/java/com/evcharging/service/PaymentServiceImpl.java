package com.evcharging.service;

import com.evcharging.model.Payment;
import com.evcharging.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Override
    public Payment processPayment(Payment payment) {
        payment.setPaymentTime(LocalDateTime.now());
        if (payment.getBooking() != null) {
            payment.getBooking().setStatus(com.evcharging.model.Booking.Status.PAID);
        }
        return paymentRepository.save(payment);
    }

    @Override
    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    @Override
    public Payment getPaymentById(Long id) {
        return paymentRepository.findById(id).orElse(null);
    }

	@Override
	public void processPayment(Long bookingId, Double amount) {
		// TODO Auto-generated method stub
		
	}
}
