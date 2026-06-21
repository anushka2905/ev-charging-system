package com.evcharging.controller;

import com.evcharging.dto.StationSummaryDTO;
import com.evcharging.model.Booking;
import com.evcharging.model.ChargingSlot;
import com.evcharging.model.ChargingStation;
import com.evcharging.model.User;
import com.evcharging.service.BookingService;
import com.evcharging.service.ChargingSlotService;
import com.evcharging.service.ChargingStationService;
import com.evcharging.service.UserService;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final ChargingStationService chargingStationService;
    private final ChargingSlotService chargingSlotService;
    private final UserService userService;

    // Show station details and slots
    @GetMapping("/stations/{stationId}")
    public String viewStationDetails(
            @PathVariable Long stationId,
            Model model,
            Authentication authentication) {

        ChargingStation station = chargingStationService.getStationById(stationId);
        List<ChargingSlot> slots = chargingSlotService.getSlotsByStationId(stationId);

        model.addAttribute("station", station);
        model.addAttribute("slots", slots);

        if (authentication != null) {
            User user = userService.findByUsername(authentication.getName());
            List<Booking> userBookings = bookingService.getBookingsByUserAndStation(user.getId(), stationId);
            List<Long> bookedSlotIds = userBookings.stream()
                                                   .map(b -> b.getSlot().getId())
                                                   .toList();
            model.addAttribute("bookedSlotIds", bookedSlotIds);
        }

        return "user/station-details";
    }

    @PostMapping("/users/book-slot")
    public String bookSlot(@RequestParam Long slotId,
                           @RequestParam Long stationId,
                           Authentication authentication,
                           RedirectAttributes redirectAttributes) {
        try {
            String username = authentication.getName();
            Booking booking = bookingService.bookSlot(slotId, username);

            redirectAttributes.addFlashAttribute("bookingSuccess", true);
            redirectAttributes.addFlashAttribute("bookingId", booking.getId());
            redirectAttributes.addFlashAttribute("bookingSlotId", slotId);
            redirectAttributes.addFlashAttribute("success", "Slot booked successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error booking slot: " + e.getMessage());
        }

        return "redirect:/bookings/stations/" + stationId;
    }

    // Show payment page
    @GetMapping("/user/payment/{bookingId}")
    public String showPaymentPage(@PathVariable Long bookingId, Model model) {
        Booking booking = bookingService.getBookingById(bookingId);
        model.addAttribute("booking", booking);
        return "user/payment";
    }

    @PostMapping("/user/process-payment/{id}")
    public String processPayment(@PathVariable("id") Long bookingId, Model model) {
        Booking booking = bookingService.getBookingById(bookingId);
        if (booking != null) {
            booking.setStatus(Booking.Status.PAID);
            bookingService.createBooking(booking); // save updated status
            model.addAttribute("booking", booking);
            return "redirect:/bookings/user/payment-success/" + bookingId;
        }
        return "redirect:/bookings/user";
    }

    @GetMapping("/user/payment-success/{id}")
    public String paymentSuccess(@PathVariable Long id, Model model) {
        Booking booking = bookingService.getBookingById(id);
        model.addAttribute("booking", booking);
        return "user/payment-success";
    }

    @GetMapping("/user/history")
    public String viewBookingHistory(Authentication authentication, Model model) {
        String username = authentication.getName();
        User user = userService.findByUsername(username);
        List<Booking> bookings = bookingService.getBookingsByUser(user);
        model.addAttribute("bookings", bookings);

        List<ChargingStation> stations = chargingStationService.getAllStations();
        List<StationSummaryDTO> stationData = stations.stream()
                .map(station -> StationSummaryDTO.builder()
                        .name(station.getName())
                        .totalSlots(bookingService.countBookingsByStation(station.getId()))
                        .availableSlots(bookingService.countPaidBookingsByStation(station.getId()))
                        .build())
                .collect(Collectors.toList());

        model.addAttribute("stationData", stationData);
        return "user/booking-history";
    }

    @GetMapping("/user/payments")
    public String viewUserPayments(Authentication authentication, Model model) {
        String username = authentication.getName();
        User user = userService.findByUsername(username);

        List<Booking> paidBookings = bookingService.getBookingsByUser(user)
                .stream()
                .filter(b -> b.getStatus() == Booking.Status.PAID)
                .toList();

        model.addAttribute("bookings", paidBookings);
        return "user/payment-history";
    }

    @GetMapping("/user/dashboard")
    public String userDashboard(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }

        String username = principal.getName();
        if (username == null) {
            return "redirect:/login";
        }

        model.addAttribute("username", username);
        return "user/dashboard";
    }
}
