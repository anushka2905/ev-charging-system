package com.evcharging.controller;

import com.evcharging.model.Booking;
import com.evcharging.model.Payment;
import com.evcharging.service.BookingService;
import com.evcharging.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

// NEW: HTML controller
@Controller
@RequestMapping("/payments/ui")
public class PaymentUIController {
    @Autowired
    private PaymentService paymentService;

    @GetMapping("/pay")
public String showPaymentForm(@RequestParam Long bookingId, Model model) {
    	com.evcharging.model.Payment payment = new com.evcharging.model.Payment();

    Booking booking = new Booking();
    booking.setId(bookingId);
    payment.setBooking(booking);
    model.addAttribute("payment", payment);
    return "payment"; // renders templates/payment.html
}

@PostMapping("/submit")
public String submitPayment(@ModelAttribute Payment payment) {
    paymentService.processPayment(payment);
    return "redirect:/success";
}
}
