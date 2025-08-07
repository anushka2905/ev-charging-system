package com.evcharging.controller;

import com.evcharging.model.*;
import com.evcharging.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/user")
public class UserDashboardController {

    @Autowired
    private ChargingStationService stationService;

    @Autowired
    private ChargingSlotService slotService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserService userService;

    // 🏠 Dashboard
    @GetMapping("/dashboard")
    public String userDashboard(Model model) {
        List<ChargingStation> stations = stationService.getAllStations();
        model.addAttribute("stations", stations);
        return "user/dashboard";
    }

    // 📅 View Slots of a Station
    @GetMapping("/station/{stationId}/slots")
    public String viewSlots(@PathVariable Long stationId, Model model) {
        ChargingStation station = stationService.getStationById(stationId);
        List<ChargingSlot> slots = slotService.getSlotsByStationId(stationId);
        model.addAttribute("station", station);
        model.addAttribute("slots", slots);
        return "user/slots";
    }

    // 📌 Book a Slot
    @PostMapping("/book/{slotId}")
    public String bookSlot(@PathVariable Long slotId, @AuthenticationPrincipal UserDetails userDetails) {
        bookingService.bookSlot(slotId, userDetails.getUsername());
        return "redirect:/user/bookings";
    }

    // 📖 View Bookings
    @GetMapping("/bookings")
    public String viewBookings(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        List<Booking> bookings = bookingService.getBookingsByUsername(userDetails.getUsername());
        model.addAttribute("bookings", bookings);
        return "user/bookings";
    }
}
