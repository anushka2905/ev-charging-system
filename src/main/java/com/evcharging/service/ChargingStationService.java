package com.evcharging.service;

import com.evcharging.model.ChargingStation;
import java.util.List;

public interface ChargingStationService {
    ChargingStation saveStation(ChargingStation station);
    List<ChargingStation> getAllStations();
    ChargingStation getStationById(Long id);
    void deleteStation(Long id);

    // 🔍 Add this for filtering/search
    List<ChargingStation> searchStations(String city, String name);
    void deleteStationById(Long id);

	List<ChargingStation> searchByNameAndCity(String name, String city);

    ChargingStation createStation(ChargingStation station);
    
}
