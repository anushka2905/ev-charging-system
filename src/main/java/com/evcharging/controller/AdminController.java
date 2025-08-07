package com.evcharging.controller;

import com.evcharging.model.User;
import com.evcharging.model.ChargingStation;
import com.evcharging.model.Booking;
import com.evcharging.service.UserService;
import com.evcharging.service.ChargingStationService;
import com.evcharging.service.BookingService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private ChargingStationService stationService;

    @Autowired
    private BookingService bookingService;

    // Admin Home
    @GetMapping("/dashboard")
    public String adminDashboard() {
        return "admin/dashboard";
    }

    // View All Users
    @GetMapping("/users")
    public String viewUsers(Model model) {
        List<User> users = userService.getAllUsers();
        model.addAttribute("users", users);
        return "admin/users";
    }

    // View All Charging Stations
    @GetMapping("/stations")
    public String viewStations(Model model) {
        List<ChargingStation> stations = stationService.getAllStations();
        model.addAttribute("stations", stations);
        return "admin/stations";
    }

    // View All Bookings
    @GetMapping("/bookings")
    public String viewBookings(Model model) {
        List<Booking> bookings = bookingService.getAllBookings();
        model.addAttribute("bookings", bookings);
        return "admin/bookings";
    }

    // Delete User
    @GetMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable Long id) {
        userService.deleteUserById(id);
        return "redirect:/admin/users";
    }

    // Delete Station
    @GetMapping("/stations/delete/{id}")
    public String deleteStation(@PathVariable Long id) {
        stationService.deleteStationById(id);
        return "redirect:/admin/stations";
    }
    @GetMapping("/station/delete/{id}")
    public String deleteStation(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        stationService.deleteStationById(id);
        redirectAttributes.addFlashAttribute("success", "Station deleted successfully!");
        return "redirect:/admin/stations";
    }


}
