package com.evcharging.dto;

import com.evcharging.model.ChargingSlot;
import lombok.*;

/**
 * DTO for Natural Language Booking (Phase 4).
 * The AI parser fills this from a user's free-text booking request.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NLBookingRequest {

    /** Original natural language text from the user */
    private String nlText;

    /** Resolved or requested station name */
    private String stationName;

    /** Resolved city/location */
    private String city;

    /** Preferred charger type parsed from NL */
    private ChargingSlot.ChargerType preferredChargerType;

    /** Requested start time (ISO-8601 string, AI-parsed) */
    private String requestedStartTime;

    /** Duration in hours (AI-parsed) */
    private double durationHours;

    /** Slot ID if the AI resolved a specific slot */
    private Long resolvedSlotId;

    /** Station ID if the AI resolved a specific station */
    private Long resolvedStationId;
}
