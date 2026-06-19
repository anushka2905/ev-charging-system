package com.evcharging.service;

import com.evcharging.dto.StationSummaryDTO;
import com.evcharging.exception.ResourceNotFoundException;
import com.evcharging.model.ChargingStation;
import com.evcharging.repository.ChargingSlotRepository;
import com.evcharging.repository.ChargingStationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ChargingStationServiceImpl — Phase 0 refactored.
 *
 * Key improvements:
 *  - Constructor injection (@RequiredArgsConstructor) instead of @Autowired field injection
 *  - Removed duplicate stationRepository field (was both stationRepo and stationRepository)
 *  - ResourceNotFoundException instead of bare RuntimeException
 *  - Added Phase 0 multi-city/geo methods
 *  - Added Haversine distance calculation for nearby search
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ChargingStationServiceImpl implements ChargingStationService {

    private final ChargingStationRepository stationRepository;
    private final ChargingSlotRepository slotRepository;

    // ── Legacy methods (kept intact for backward compat) ──────────

    @Override
    public ChargingStation saveStation(ChargingStation station) {
        log.info("Saving station: {}", station.getName());
        return stationRepository.save(station);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChargingStation> getAllStations() {
        return stationRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public ChargingStation getStationById(Long id) {
        return stationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ChargingStation", id));
    }

    @Override
    public void deleteStation(Long id) {
        stationRepository.deleteById(id);
    }

    @Override
    public void deleteStationById(Long id) {
        stationRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChargingStation> searchStations(String city, String name) {
        return stationRepository.findByCityContainingIgnoreCaseAndNameContainingIgnoreCase(city, name);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChargingStation> searchByNameAndCity(String name, String city) {
        return stationRepository.findByNameContainingIgnoreCaseAndCityContainingIgnoreCase(name, city);
    }

    @Override
    public ChargingStation createStation(ChargingStation station) {
        return stationRepository.save(station);
    }

    // ── Phase 0 additions ─────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<ChargingStation> searchByAny(String query) {
        return stationRepository.searchByAny(query);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StationSummaryDTO> findNearby(double lat, double lon, double radiusKm) {
        // 1° lat ≈ 111 km; 1° lon ≈ 111 * cos(lat) km
        double deltaLat = radiusKm / 111.0;
        double deltaLon = radiusKm / (111.0 * Math.cos(Math.toRadians(lat)));

        List<ChargingStation> candidates = stationRepository.findWithinBoundingBox(
                lat - deltaLat, lat + deltaLat,
                lon - deltaLon, lon + deltaLon);

        return candidates.stream()
                .map(s -> {
                    StationSummaryDTO dto = toSummaryDTO(s);
                    dto.setDistanceKm(haversineKm(lat, lon, s.getLatitude(), s.getLongitude()));
                    return dto;
                })
                .filter(dto -> dto.getDistanceKm() <= radiusKm)
                .sorted((a, b) -> Double.compare(a.getDistanceKm(), b.getDistanceKm()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChargingStation> getStationsByState(String state) {
        return stationRepository.findByStateIgnoreCase(state);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getAllCities() {
        return stationRepository.findAllDistinctCities();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getAllStates() {
        return stationRepository.findAllDistinctStates();
    }

    @Override
    @Transactional(readOnly = true)
    public int countAvailableSlots(Long stationId) {
        return slotRepository.countAvailableByStation(stationId);
    }

    @Override
    @Transactional(readOnly = true)
    public StationSummaryDTO toSummaryDTO(ChargingStation s) {
        int available = slotRepository.countAvailableByStation(s.getId());
        return StationSummaryDTO.builder()
                .id(s.getId())
                .name(s.getName())
                .city(s.getCity())
                .state(s.getState())
                .latitude(s.getLatitude())
                .longitude(s.getLongitude())
                .availableSlots(available)
                .totalSlots(s.getTotalSlots())
                .pricePerUnit(s.getPricePerUnit())
                .chargerTypes(s.getChargerTypes())
                .rating(s.getRating())
                .operationalStatus(s.getOperationalStatus() != null
                        ? s.getOperationalStatus().name() : "ACTIVE")
                .operator(s.getOperator())
                .build();
    }

    // ── Haversine formula ─────────────────────────────────────────

    /**
     * Returns the distance in kilometers between two GPS coordinates.
     * Used in findNearby() and Phase 3 Recommendation Engine.
     */
    public static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0; // Earth radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
