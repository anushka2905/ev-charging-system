package com.evcharging.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * ChargingSlot entity — Phase 0 enhanced version.
 *
 * Changes from v1:
 *  - Added chargerType (AC_SLOW, DC_FAST, CCS2, CHAdeMO, TYPE2)
 *  - Added powerKw (kilowatt rating)
 *  - Separated "available" and "booked" into a single SlotStatus enum
 *  - Added lastBookedAt for predictive analytics (Phase 6)
 *  - Added connectorId for physical identification
 */
@Entity
@Table(name = "charging_slots",
       indexes = {
           @Index(name = "idx_slot_station", columnList = "station_id"),
           @Index(name = "idx_slot_status",  columnList = "status")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChargingSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String slotName;

    /**
     * Physical connector identifier on the charger unit.
     * Helps in larger stations with multiple connectors.
     */
    @Column(length = 50)
    private String connectorId;

    /**
     * AVAILABLE | BOOKED | IN_USE | MAINTENANCE
     * Replaces the old dual boolean (available + booked).
     * Backward-compat helpers isAvailable() / isBooked() preserved below.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SlotStatus status = SlotStatus.AVAILABLE;

    /** Charger type — key for recommendation filtering (Phase 3) */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private ChargerType chargerType = ChargerType.AC_SLOW;

    /** Power rating in kW (e.g. 3.3, 7.2, 50, 150) */
    @Column
    @Builder.Default
    private double powerKw = 7.2;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id", nullable = false)
    private ChargingStation chargingStation;

    /** For predictive analytics — when was this slot last booked */
    private LocalDateTime lastBookedAt;

    // ── Enums ────────────────────────────────────────────────────

    public enum SlotStatus {
        AVAILABLE, BOOKED, IN_USE, MAINTENANCE
    }

    public enum ChargerType {
        AC_SLOW("AC Slow (3.3–7.2 kW)"),
        AC_FAST("AC Fast (11–22 kW)"),
        DC_FAST("DC Fast (50 kW)"),
        DC_ULTRA_FAST("DC Ultra Fast (100–350 kW)"),
        CCS2("CCS2 Combo"),
        CHAdeMO("CHAdeMO"),
        TYPE2("Type-2 AC");

        private final String displayName;
        ChargerType(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    // ── Backward-compatibility helpers ───────────────────────────

    /** @deprecated use status == AVAILABLE */
    @Deprecated(since = "2.0", forRemoval = false)
    public boolean isAvailable() {
        return this.status == SlotStatus.AVAILABLE;
    }

    /** @deprecated use status == BOOKED */
    @Deprecated(since = "2.0", forRemoval = false)
    public boolean isBooked() {
        return this.status == SlotStatus.BOOKED || this.status == SlotStatus.IN_USE;
    }

    /** @deprecated use setStatus(SlotStatus.BOOKED) */
    @Deprecated(since = "2.0", forRemoval = false)
    public void setBooked(boolean booked) {
        this.status = booked ? SlotStatus.BOOKED : SlotStatus.AVAILABLE;
    }

    /** @deprecated use setStatus(SlotStatus.AVAILABLE) */
    @Deprecated(since = "2.0", forRemoval = false)
    public void setAvailable(boolean available) {
        if (available) this.status = SlotStatus.AVAILABLE;
    }
}
