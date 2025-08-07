package com.evcharging.controller;

import com.evcharging.model.Booking;
import com.evcharging.model.ChargingSlot;
import com.evcharging.model.User;
import com.evcharging.service.BookingService;
import com.evcharging.service.ChargingSlotService;
import com.evcharging.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/booking")
public class BookingUIController {

    @Autowired
    private ChargingSlotService chargingSlotService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserService userService;

    // Step 1: Show all available slots
    @GetMapping("/slots")
    public String viewAvailableSlots(Model model) {
        List<ChargingSlot> slots = chargingSlotService.getAllSlots();
        model.addAttribute("slots", slots);
        return "book-slot"; // Thymeleaf template name
    }

    // Step 2: Book the selected slot
    @PostMapping("/book/{slotId}")
    public String bookSlot(@PathVariable Long slotId, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName(); // Assuming username is email
        User user = userService.getUserByEmail(username);

        ChargingSlot slot = chargingSlotService.getSlotById(slotId);

        if (slot == null || slot.isBooked()) {
            model.addAttribute("error", "Slot is unavailable!");
            return "book-slot";
        }

        Booking booking = bookingService.bookSlot(user, slot);
        model.addAttribute("booking", booking);

        return "redirect:/booking/pay/" + booking.getId(); // Redirect to payment
    }

    // Step 3: Pay for the booked slot
    @GetMapping("/pay/{bookingId}")
    public String payForSlot(@PathVariable Long bookingId, Model model) {
        Booking booking = bookingService.getBookingById(bookingId);
        model.addAttribute("booking", booking);
        return "payment"; // payment.html
    }

    @PostMapping("/pay/{bookingId}")
    public String confirmPayment(@PathVariable Long bookingId, Model model) {
        bookingService.markAsPaid(bookingId);
        return "redirect:/booking/history";
    }

    // Step 4: See booking history
    @GetMapping("/history")
    public String viewBookingHistory(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User user = userService.getUserByEmail(username);

        List<Booking> bookings = bookingService.getBookingsForUser(user);
        model.addAttribute("bookings", bookings);

        return "booking-history"; // booking-history.html
    }
}
