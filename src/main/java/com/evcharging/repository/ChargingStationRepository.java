package com.evcharging.repository;

import com.evcharging.model.ChargingStation;
import com.evcharging.model.ChargingStation.OperationalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * ChargingStation Repository — Phase 0 enhanced version.
 * Added multi-city, state, and radius-based queries.
 */
public interface ChargingStationRepository extends JpaRepository<ChargingStation, Long> {

    // ── Legacy (backward-compat) ──────────────────────────────────
    List<ChargingStation> findByCity(String city);
    List<ChargingStation> findByCityContainingIgnoreCase(String city);
    List<ChargingStation> findByNameContainingIgnoreCase(String name);
    List<ChargingStation> findByCityContainingIgnoreCaseAndNameContainingIgnoreCase(String city, String name);
    List<ChargingStation> findByNameContainingIgnoreCaseAndCityContainingIgnoreCase(String name, String city);

    // ── Phase 0: Multi-city/state support ─────────────────────────
    List<ChargingStation> findByStateIgnoreCase(String state);
    List<ChargingStation> findByCountryIgnoreCase(String country);
    List<ChargingStation> findByOperationalStatus(OperationalStatus status);

    /** Search by city OR state (useful for broader area search) */
    @Query("SELECT s FROM ChargingStation s WHERE " +
           "LOWER(s.city) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.state) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<ChargingStation> searchByAny(@Param("query") String query);

    /** Phase 3: Find stations within a bounding box (fast geo-query without Haversine) */
    @Query("SELECT s FROM ChargingStation s WHERE " +
           "s.latitude  BETWEEN :minLat AND :maxLat AND " +
           "s.longitude BETWEEN :minLon AND :maxLon AND " +
           "s.operationalStatus = 'ACTIVE'")
    List<ChargingStation> findWithinBoundingBox(
            @Param("minLat") double minLat,
            @Param("maxLat") double maxLat,
            @Param("minLon") double minLon,
            @Param("maxLon") double maxLon);

    /** Phase 3: Count available slots per station */
    @Query("SELECT COUNT(sl) FROM ChargingSlot sl WHERE sl.chargingStation.id = :stationId AND sl.status = 'AVAILABLE'")
    int countAvailableSlots(@Param("stationId") Long stationId);

    /** Phase 6: All distinct cities (for admin dashboard analytics) */
    @Query("SELECT DISTINCT s.city FROM ChargingStation s WHERE s.city IS NOT NULL ORDER BY s.city")
    List<String> findAllDistinctCities();

    /** Phase 6: All distinct states */
    @Query("SELECT DISTINCT s.state FROM ChargingStation s WHERE s.state IS NOT NULL ORDER BY s.state")
    List<String> findAllDistinctStates();
}
