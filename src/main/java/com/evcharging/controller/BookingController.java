package com.evcharging.controller;

import com.evcharging.model.Booking;
import com.evcharging.model.ChargingSlot;
import com.evcharging.model.ChargingStation;
import com.evcharging.model.User;
import com.evcharging.service.BookingService;
import com.evcharging.service.ChargingSlotService;
import com.evcharging.service.ChargingStationService;
import com.evcharging.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private ChargingStationService chargingStationService;

    // ✅ REST API — Create Booking (JSON)
    @PostMapping
    @ResponseBody
    public Booking createBooking(@RequestBody Booking booking) {
        return bookingService.createBooking(booking);
    }

    // ✅ REST API — Get All Bookings (JSON)
    @GetMapping
    @ResponseBody
    public List<Booking> getAllBookings() {
        return bookingService.getAllBookings();
    }

    // ✅ REST API — Get Booking by ID
    @GetMapping("/{id}")
    @ResponseBody
    public Booking getBookingById(@PathVariable Long id) {
        return bookingService.getBookingById(id);
    }

    // ✅ REST API — Delete Booking
    @DeleteMapping("/{id}")
    @ResponseBody
    public void deleteBooking(@PathVariable Long id) {
        bookingService.deleteBooking(id);
    }

    // ✅ REST API — Book a Slot by slotId and userId
    @PostMapping("/book")
    @ResponseBody
    public Booking bookSlot(@RequestParam Long slotId, @RequestParam Long userId) {
        return bookingService.bookSlot(slotId, userId);
    }

    // ✅ REST API — Get Booking History by User ID
    @GetMapping("/history/{username}")
    public List<Booking> getBookingHistory(@PathVariable String username) {
        return bookingService.getBookingsByUsername(username);
    }
    
    @Autowired
    private UserService userService; 
    
    @PostMapping("/users/book-slot")
    public String bookSlot(@RequestParam Long slotId,
                           @RequestParam Long stationId,
                           RedirectAttributes redirectAttributes,
                           Authentication authentication) {
        try {
        	String username = authentication.getName();
        	User user = userService.findByUsername(username);

            
            bookingService.bookSlot(slotId, user); // This method should update slot + save booking
            
            redirectAttributes.addFlashAttribute("success", "Slot booked successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error while booking slot: " + e.getMessage());
        }

        return "redirect:/user/station/" + stationId;
    }



    @GetMapping("/user/payment/{bookingId}")
    public String showPaymentPage(@PathVariable Long bookingId, Model model) {
        Booking booking = bookingService.getBookingById(bookingId);
        model.addAttribute("booking", booking);
        return "user/payment";  // payment.html under templates/user/
    }


    @PostMapping("/user/process-payment/{bookingId}")
    public String processPayment(@PathVariable Long bookingId) {
        bookingService.markAsPaid(bookingId);
        return "redirect:/user/bookings"; // Booking history page (we can make this next)
    }
    
    @PostMapping("/users/make-payment")
    public String makePayment(@RequestParam Long bookingId, Model model) {
        // For now, simulate a successful payment
        bookingService.markAsPaid(bookingId); // Create this method to set status = PAID
        model.addAttribute("success", "Payment completed successfully!");
        return "redirect:/users/bookings"; // or show payment confirmation
    }



    @PostMapping("/user/markPaid/{bookingId}")
    public String markBookingAsPaid(@PathVariable Long bookingId) {
        bookingService.markAsPaid(bookingId);
        return "redirect:/user/bookings"; // Redirect to booking history or dashboard
    }

    
    // ✅ View Controller — Show slot booking page for selected station
    @PostMapping("/stations/select")
    public String selectStation(@RequestParam Long stationId, Model model) {
        ChargingStation station = chargingStationService.getStationById(stationId);
        model.addAttribute("selectedStation", station);
        return "slotBooking"; // Make sure this Thymeleaf view exists in /templates
    }
    

}
