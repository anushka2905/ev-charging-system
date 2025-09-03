package com.evcharging.controller;

import com.evcharging.model.Booking;
import com.evcharging.model.ChargingSlot;
import com.evcharging.model.ChargingStation;
import com.evcharging.model.User;
import com.evcharging.repository.ChargingSlotRepository;
import com.evcharging.repository.UserRepository;
import com.evcharging.service.BookingService;
import com.evcharging.service.ChargingSlotService;
import com.evcharging.service.ChargingStationService;
import com.evcharging.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.Principal;
import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


@Controller
@RequestMapping("/users")
public class UserController {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private ChargingStationService chargingStationService;

    @Autowired
    private ChargingSlotService chargingSlotService;

    @Autowired
    private BookingService bookingService;

    // Registration form
    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute("user") User user, Model model) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            model.addAttribute("error", "Username already exists");
            return "register";
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        if (user.getRole() == null || user.getRole().isEmpty()) {
            user.setRole("ROLE_USER");
        }

        userRepository.save(user);
        return "redirect:/login?registerSuccess";
    }

    // Dashboard - list all stations
    @GetMapping("/dashboard")
    public String userDashboard(Model model) {
        List<ChargingStation> stations = chargingStationService.getAllStations();
        model.addAttribute("stations", stations);
        return "user/dashboard";
    }

    // View station details & slots
    @GetMapping("/stations/select/{id}")
    public String selectStation(@PathVariable Long id, Model model,
                                @ModelAttribute("bookingId") String bookingId,
                                @ModelAttribute("success") String success,
                                @ModelAttribute("error") String error) {
        ChargingStation station = chargingStationService.getStationById(id);
        List<ChargingSlot> slots = chargingSlotService.getSlotsByStationId(id);

        model.addAttribute("station", station);
        model.addAttribute("slots", slots);
        
        if (bookingId != null && !bookingId.isEmpty()) {
            model.addAttribute("bookingId", bookingId);
        }
        if (success != null) model.addAttribute("success", success);
        if (error != null) model.addAttribute("error", error);

        return "user/station-details";
    }

    
    @GetMapping("/stations")
    public String stationList(Model model) {
        List<ChargingStation> stations = chargingStationService.getAllStations();
        model.addAttribute("stations", stations);
        return "user/stations";
    }

    @PostMapping("/book-slot")
    public String bookSlot(@RequestParam("slotId") Long slotId,
                           @RequestParam("stationId") Long stationId,
                           Principal principal,
                           RedirectAttributes redirectAttributes) {

        String username = principal.getName(); // ✅ Get logged-in username
        try {
            bookingService.bookSlot(slotId, username); // ✅ Now this must take slotId + username
            redirectAttributes.addFlashAttribute("success", "Slot booked successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to book slot: " + e.getMessage());
        }

        return "redirect:/users/stations/select/" + stationId; // ✅ Correct redirect
    }





    @Autowired
    private ChargingSlotRepository chargingSlotRepository;

    @GetMapping("/stations/{stationId}/slots")
    public String viewSlots(@PathVariable Long stationId, Model model) {
        ChargingStation station = chargingStationService.getStationById(stationId);
        List<ChargingSlot> slots = chargingSlotService.getSlotsByStationId(stationId);
        model.addAttribute("stationName", station.getName());
        model.addAttribute("slots", slots);

        return "slots";
    }

    
    @GetMapping("/users/stations/info")
    public String getStations(Model model) throws Exception {
        List<ChargingStation> stations = chargingStationService.getAllStations();
        
        ObjectMapper mapper = new ObjectMapper();
        String stationsJson = mapper.writeValueAsString(stations);

        model.addAttribute("stations", stations);
        model.addAttribute("stationsJson", stationsJson);
        return "user/stations";
    }
    
    @GetMapping("/user/station/{stationId}")
    public String getStationDetails(@PathVariable Long stationId, Model model) {
        ChargingStation station = chargingStationService.getStationById(stationId);
        List<ChargingSlot> slots = chargingSlotService.getSlotsByStationId(stationId);

        model.addAttribute("station", station);
        model.addAttribute("slots", slots);
        return "user/station-details";
    }
    
    @GetMapping("/users/stations")
    public String viewStations(Model model) {
        List<ChargingStation> stations = chargingStationService.getAllStations();
        model.addAttribute("stations", stations);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        Booking latestBooking = bookingService.getLatestBookingForUser(username);
        if (latestBooking != null) {
            model.addAttribute("latestBookingId", latestBooking.getId());
        }

        return "user/station-list"; // or station-details, depending on your file
    }
    
    @PostMapping("/users/payment")
    public String payForBooking(@RequestParam("bookingId") Long bookingId, RedirectAttributes redirectAttributes) {
        bookingService.markAsPaid(bookingId);
        redirectAttributes.addFlashAttribute("success", "Payment successful!");
        return "redirect:/users/stations";
    }
    
    @GetMapping("/payment")
    public String showPaymentPage(@RequestParam Long bookingId, Model model) {
        Booking booking = bookingService.getBookingById(bookingId);
        model.addAttribute("booking", booking);
        return "users/payment"; // Your existing payment.html page
    }
    
    @PostMapping("/payment")
    public String processPayment(@RequestParam Long bookingId) {
        bookingService.markAsPaid(bookingId);
        return "redirect:/user/dashboard"; // or show receipt page
    }

    
    @PostMapping("/user/bookSlot")
    public String bookSlot(@RequestParam Long slotId, Model model, Principal principal) {
        String username = principal.getName();
        bookingService.bookSlot(slotId, username);

        Booking latestBooking = bookingService.getLatestBookingForUser(username);
        model.addAttribute("booking", latestBooking);

        return "user/booking-success";  // In this template, add the "Make Payment" button
    }
    @GetMapping("/users/bookings")
    public String viewUserBookings(Authentication authentication, Model model) {
        User user = userService.findByUsername(authentication.getName());
        List<Booking> bookings = bookingService.getBookingsByUser(user.getId());
        model.addAttribute("bookings", bookings);
        return "user/bookings";  // Create templates/user/bookings.html
    }


}
