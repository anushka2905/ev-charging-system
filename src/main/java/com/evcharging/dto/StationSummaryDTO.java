package com.evcharging.dto;

import lombok.*;

/**
 * Keeps the original StationSummaryDTO for backward compatibility
 * (used in map/station-list views) and extends it with AI-phase fields.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StationSummaryDTO {

    private Long id;
    private String name;
    private String city;
    private String state;
    private double latitude;
    private double longitude;
    private int availableSlots;
    private int totalSlots;
    private double pricePerUnit;
    private String chargerTypes;
    private double rating;
    private String operationalStatus;
    private String operator;

    /** Phase 3: distance from user's current location in km */
    private double distanceKm;

    /** Phase 3: AI recommendation score (0–100) */
    private double recommendationScore;

    /** Phase 3: AI explanation for this recommendation */
    private String recommendationReason;
}
