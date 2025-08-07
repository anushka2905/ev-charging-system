package com.evcharging.controller;

import com.evcharging.model.ChargingStation;
import com.evcharging.service.ChargingStationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class WebViewController {

    @Autowired
    private ChargingStationService chargingStationService;

    @GetMapping("/view-stations")
    public String viewStations(Model model) {
        List<ChargingStation> stations = chargingStationService.getAllStations();
        model.addAttribute("stations", stations);
        return "stations"; // This maps to stations.html under templates
    }
}
