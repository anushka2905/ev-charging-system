package com.evcharging.dto;

import lombok.*;

/**
 * DTO used when creating/updating a ChargingStation via the admin panel.
 * Decouples HTTP form data from the entity — a clean-architecture requirement.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChargingStationDTO {

    private Long id;
    private String name;
    private String address;
    private String city;
    private String state;
    private String country;
    private double latitude;
    private double longitude;
    private String chargerTypes;   // comma-separated
    private int totalSlots;
    private double pricePerUnit;
    private String operator;
    private String operationalStatus;
}
