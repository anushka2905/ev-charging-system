package com.evcharging.service;

import com.evcharging.dto.StationSummaryDTO;
import com.evcharging.model.ChargingStation;
import java.util.List;

/**
 * ChargingStationService — Phase 0 enhanced interface.
 * All legacy methods are preserved for backward compatibility.
 */
public interface ChargingStationService {

    // ── Legacy (backward-compat) ──────────────────────────────────
    ChargingStation saveStation(ChargingStation station);
    List<ChargingStation> getAllStations();
    ChargingStation getStationById(Long id);
    void deleteStation(Long id);
    void deleteStationById(Long id);
    List<ChargingStation> searchStations(String city, String name);
    List<ChargingStation> searchByNameAndCity(String name, String city);
    ChargingStation createStation(ChargingStation station);

    // ── Phase 0 additions ─────────────────────────────────────────

    /** Full-text search across name, city, state */
    List<ChargingStation> searchByAny(String query);

    /** Stations near a GPS coordinate within given km radius */
    List<StationSummaryDTO> findNearby(double lat, double lon, double radiusKm);

    /** Stations filtered by state */
    List<ChargingStation> getStationsByState(String state);

    /** All distinct cities that have stations */
    List<String> getAllCities();

    /** All distinct states */
    List<String> getAllStates();

    /** Count of available slots for a station */
    int countAvailableSlots(Long stationId);

    /** Convert entity to summary DTO (with slot count populated) */
    StationSummaryDTO toSummaryDTO(ChargingStation station);
}
