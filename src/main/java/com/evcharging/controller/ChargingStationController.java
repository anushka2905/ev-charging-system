package com.evcharging.controller;

import com.evcharging.dto.StationSummaryDTO;
import com.evcharging.model.ChargingSlot;
import com.evcharging.model.ChargingStation;
import com.evcharging.service.BookingService;
import com.evcharging.service.ChargingSlotService;
import com.evcharging.service.ChargingStationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/stations")
public class ChargingStationController {

    @Autowired
    private ChargingStationService chargingStationService;

    // Create a new station (REST API)
    @PostMapping
    public ResponseEntity<ChargingStation> createStation(@Validated @RequestBody ChargingStation station) {
        ChargingStation saved = chargingStationService.saveStation(station);
        return ResponseEntity.ok(saved);
    }

    // Fetch all stations (REST API)
    @GetMapping
    public ResponseEntity<List<ChargingStation>> getAllStations() {
        List<ChargingStation> list = chargingStationService.getAllStations();
        return ResponseEntity.ok(list);
    }

    // Fetch station by ID (REST API)
    @GetMapping("/{id}")
    public ResponseEntity<ChargingStation> getStationById(@PathVariable Long id) {
        ChargingStation station = chargingStationService.getStationById(id);
        if (station == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(station);
    }

    // Delete station from Admin Dashboard (Thymeleaf)
    @PostMapping("/delete/{id}")
    public String deleteStationFromDashboard(@PathVariable Long id) {
        chargingStationService.deleteStation(id);
        return "redirect:/stations/admin";
    }

    // Show Google Map with stations
    @GetMapping("/map")
    public String showMap(Model model) throws Exception {
        List<ChargingStation> stations = chargingStationService.getAllStations();
        ObjectMapper mapper = new ObjectMapper();
        String stationsJson = mapper.writeValueAsString(stations);
        model.addAttribute("stationsJson", stationsJson);
        return "map"; // View: map.html
    }

    // REST API for search (used in AJAX or React frontends)
    @GetMapping("/search")
    public ResponseEntity<List<ChargingStation>> searchStations(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String name) {
        List<ChargingStation> stations = chargingStationService.searchStations(city, name);
        return ResponseEntity.ok(stations);
    }

    // Admin Dashboard page with optional filter
    @GetMapping("/admin")
    public String showAdminDashboard(Model model,
                                     @RequestParam(required = false) String city,
                                     @RequestParam(required = false) String name) {
        List<ChargingStation> stations = chargingStationService.searchStations(city, name);
        model.addAttribute("stations", stations);
        return "admin-dashboard"; // View: admin-dashboard.html
    }

    // User-facing station list page with filter
    @GetMapping("/list")
    public String showStationList(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String city,
            Model model) {

        List<ChargingStation> stations;

        // If both filters are empty, return all stations
        if ((name == null || name.isEmpty()) && (city == null || city.isEmpty())) {
            stations = chargingStationService.getAllStations();
        } else {
            stations = chargingStationService.searchStations(
                    city != null ? city : "",
                    name != null ? name : ""
            );
        }

        model.addAttribute("stations", stations);
        model.addAttribute("name", name);
        model.addAttribute("city", city);
        return "station-list"; // View: station-list.html
    }


    // Optional: used to forward selected station to booking page
    @PostMapping("/select")
    public String selectStation(@RequestParam("stationId") Long stationId, Model model) {
        ChargingStation selectedStation = chargingStationService.getStationById(stationId);
        model.addAttribute("selectedStation", selectedStation);
        return "station-selected";  // View: station-selected.html (must exist)
    }
    
    @GetMapping("/stations/map")
    public String showStationMap(Model model) {
        List<ChargingStation> stations = chargingStationService.getAllStations();
        model.addAttribute("stations", stations);
        return "station_map";
    }
    @Autowired
    private ChargingSlotService chargingSlotService;

    @GetMapping("/details/{stationId}")
    public String getStationDetails(@PathVariable Long stationId,
                                    @RequestParam(value = "bookingSuccess", required = false) String bookingSuccess,
                                    Model model) {
        ChargingStation station = chargingStationService.getStationById(stationId);
        List<ChargingSlot> slots = chargingSlotService.getSlotsByStationId(stationId);

        model.addAttribute("station", station);
        model.addAttribute("slots", slots);

        if ("true".equals(bookingSuccess)) {
            model.addAttribute("bookingSuccess", true);
        }

        return "station-details"; // station-details.html (your page)
    }
    @PostMapping("/book-slot")
    public String bookSlot(@RequestParam Long slotId, @RequestParam Long stationId) {
        chargingSlotService.bookSlot(slotId);
        return "redirect:/stations/details/" + stationId + "?bookingSuccess=true";
    }
    
    @Autowired
    private BookingService bookingService;
    
    @GetMapping("/user/station-summary")
    public String viewStationSummary(Model model) {
        List<ChargingStation> stations = chargingStationService.getAllStations();

        List<StationSummaryDTO> stationData = stations.stream()
            .map(station -> new StationSummaryDTO(
                    station.getName(),
                    bookingService.countBookingsByStation(station.getId()),
                    bookingService.countPaidBookingsByStation(station.getId())
            ))
            .collect(Collectors.toList());

        model.addAttribute("stations", stationData);
        return "user/station-summary";
    }


}
