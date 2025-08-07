package com.evcharging.controller;

import com.evcharging.model.Payment;
import com.evcharging.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping
    public Payment processPayment(@RequestBody Payment payment) {
        return paymentService.processPayment(payment);
    }

    @GetMapping
    public List<Payment> getAllPayments() {
        return paymentService.getAllPayments();
    }

    @GetMapping("/{id}")
    public Payment getPaymentById(@PathVariable Long id) {
        return paymentService.getPaymentById(id);
    }
    @PostMapping("/payment/complete")
    public String completePayment(@RequestParam Long bookingId, @RequestParam Double amount) {
        paymentService.processPayment(bookingId, amount);
        return "redirect:/booking/history";
    }
    
    
}
