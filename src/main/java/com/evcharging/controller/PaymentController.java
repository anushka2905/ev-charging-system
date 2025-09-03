package com.evcharging.controller;

import com.evcharging.model.Payment;
import com.evcharging.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller   // <-- Change here
@RequestMapping("/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping
    @ResponseBody  // for JSON response only
    public Payment processPayment(@RequestBody Payment payment) {
        return paymentService.processPayment(payment);
    }

    @GetMapping
    @ResponseBody
    public List<Payment> getAllPayments() {
        return paymentService.getAllPayments();
    }

    @GetMapping("/{id}")
    @ResponseBody
    public Payment getPaymentById(@PathVariable Long id) {
        return paymentService.getPaymentById(id);
    }

    @PostMapping("/payment/complete")
    public String completePayment(@RequestParam Long bookingId,
                                  @RequestParam Double amount,
                                  RedirectAttributes redirectAttributes) {
        paymentService.processPayment(bookingId, amount);
        redirectAttributes.addFlashAttribute("message", "Payment successful!");
        return "redirect:/booking/history";
    }

    @GetMapping("/payment")
    public String paymentPage(@RequestParam(required = false) Long bookingId, Model model) {
        model.addAttribute("bookingId", bookingId);
        return "payment"; // Looks for payment.html
    }
}
