package com.evcharging.controller;

import com.evcharging.model.Booking;
import com.evcharging.model.Payment;
import com.evcharging.service.BookingService;
import com.evcharging.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/payments/ui")
@RequiredArgsConstructor
public class PaymentUIController {

    private final PaymentService paymentService;
    private final BookingService bookingService;

    @GetMapping("/pay")
    public String showPaymentForm(@RequestParam Long bookingId, Model model) {
        // Load the real booking from the DB instead of creating a stub
        Booking booking = bookingService.getBookingById(bookingId);

        Payment payment = new Payment();
        payment.setBooking(booking);
        model.addAttribute("payment", payment);
        model.addAttribute("booking", booking);
        return "payment"; // renders templates/payment.html
    }

    @PostMapping("/submit")
    public String submitPayment(@ModelAttribute Payment payment) {
        paymentService.processPayment(payment);
        return "redirect:/booking/history";
    }
}
