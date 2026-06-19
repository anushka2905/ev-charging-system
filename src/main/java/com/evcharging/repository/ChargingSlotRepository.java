package com.evcharging.repository;

import com.evcharging.model.ChargingSlot;
import com.evcharging.model.ChargingSlot.SlotStatus;
import com.evcharging.model.ChargingSlot.ChargerType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * ChargingSlotRepository — Phase 0 enhanced version.
 * Added status-enum queries, charger-type filter, and analytics queries.
 */
@Repository
public interface ChargingSlotRepository extends JpaRepository<ChargingSlot, Long> {

    // ── Legacy (backward-compat) ──────────────────────────────────
    List<ChargingSlot> findByChargingStation_Id(Long stationId);
    List<ChargingSlot> findByChargingStationId(Long stationId);

    /** Backward-compat: maps to status == AVAILABLE */
    default List<ChargingSlot> findByAvailableTrue() {
        return findByStatus(SlotStatus.AVAILABLE);
    }

    /** Backward-compat: maps to status != BOOKED */
    default List<ChargingSlot> findByBookedFalse() {
        return findByStatus(SlotStatus.AVAILABLE);
    }

    // ── Phase 0: Status-based ─────────────────────────────────────
    List<ChargingSlot> findByStatus(SlotStatus status);
    List<ChargingSlot> findByChargingStation_IdAndStatus(Long stationId, SlotStatus status);

    // ── Phase 3: Charger-type filter for Recommendations ──────────
    List<ChargingSlot> findByChargerTypeAndStatus(ChargerType chargerType, SlotStatus status);

    @Query("SELECT sl FROM ChargingSlot sl WHERE " +
           "sl.chargingStation.id = :stationId AND " +
           "sl.chargerType = :type AND " +
           "sl.status = 'AVAILABLE'")
    List<ChargingSlot> findAvailableByStationAndType(
            @Param("stationId") Long stationId,
            @Param("type") ChargerType type);

    /** Count available slots per station — used in recommendation ranking */
    @Query("SELECT COUNT(sl) FROM ChargingSlot sl WHERE " +
           "sl.chargingStation.id = :stationId AND sl.status = 'AVAILABLE'")
    int countAvailableByStation(@Param("stationId") Long stationId);

    /** Phase 6: Slot usage count (for occupancy analytics) */
    @Query("SELECT sl.id, sl.slotName, COUNT(b) AS usageCount " +
           "FROM ChargingSlot sl LEFT JOIN Booking b ON b.slot.id = sl.id " +
           "WHERE sl.chargingStation.id = :stationId " +
           "GROUP BY sl.id, sl.slotName ORDER BY usageCount DESC")
    List<Object[]> findSlotUsageStats(@Param("stationId") Long stationId);
}
