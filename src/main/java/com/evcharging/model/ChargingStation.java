package com.evcharging.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.time.LocalDateTime;

/**
 * ChargingStation entity — Phase 0 enhanced version.
 *
 * Changes from v1:
 *  - Removed Bhopal-specific assumption (city field already existed, now promoted)
 *  - Added state, country fields (multi-city/country support)
 *  - Added chargerTypes, totalSlots, operationalStatus
 *  - Added rating, ratingCount for recommendation engine (Phase 3)
 *  - Added pricePerUnit for cost calculations
 *  - Added createdAt / updatedAt audit fields
 *  - Added operator (who runs the station)
 *  - Removed ambiguous 'location' field (redundant with address+city)
 */
@Entity
@Table(name = "charging_stations",
       indexes = {
           @Index(name = "idx_station_city",  columnList = "city"),
           @Index(name = "idx_station_state", columnList = "state")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChargingStation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(length = 100)
    private String city;

    /** Phase 0: added for multi-city/state support */
    @Column(length = 100)
    private String state;

    /** Phase 0: added for future international expansion */
    @Column(length = 100)
    @Builder.Default
    private String country = "India";

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    /**
     * Comma-separated charger types available at this station.
     * e.g. "AC_SLOW,DC_FAST,CCS2,CHAdeMO"
     * Phase 3 uses this for filtering recommendations.
     */
    @Column(length = 200)
    private String chargerTypes;

    /** Total physical slots (computed or managed by admin) */
    @Column(nullable = false)
    @Builder.Default
    private int totalSlots = 1;

    /** Price per kWh in INR */
    @Column(nullable = false)
    @Builder.Default
    private double pricePerUnit = 12.0;

    /** Average star rating (1-5), updated by review engine */
    @Column
    @Builder.Default
    private double rating = 0.0;

    /** Number of ratings received */
    @Column
    @Builder.Default
    private int ratingCount = 0;

    /** Who operates the station (e.g., "Tata Power", "BPCL", "Ather") */
    @Column(length = 100)
    private String operator;

    /** ACTIVE | INACTIVE | MAINTENANCE */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private OperationalStatus operationalStatus = OperationalStatus.ACTIVE;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column
    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ── Enums ────────────────────────────────────────────────────

    public enum OperationalStatus {
        ACTIVE, INACTIVE, MAINTENANCE
    }

    /** Phase 3 helper: human-readable charger list */
    public String[] getChargerTypeArray() {
        if (chargerTypes == null || chargerTypes.isBlank()) return new String[0];
        return chargerTypes.split(",");
    }
}
