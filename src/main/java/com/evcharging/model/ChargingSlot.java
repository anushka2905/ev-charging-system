package com.evcharging.model;

import jakarta.persistence.*;

@Entity
public class ChargingSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String slotName;

    private boolean available;

    private boolean booked;

    @ManyToOne
    @JoinColumn(name = "station_id")
    private ChargingStation chargingStation;

    // ✅ Add no-arg constructor here
    public ChargingSlot() {
    }

    // ✅ Optionally add all-args constructor (optional, for convenience)
    public ChargingSlot(Long id, String slotName, boolean available, boolean booked, ChargingStation chargingStation) {
        this.id = id;
        this.slotName = slotName;
        this.available = available;
        this.booked = booked;
        this.chargingStation = chargingStation;
    }

    // --- Getters and Setters ---

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean isBooked() {
        return booked;
    }

    public void setBooked(boolean booked) {
        this.booked = booked;
    }

    public String getSlotName() {
        return slotName;
    }

    public void setSlotName(String slotName) {
        this.slotName = slotName;
    }

    public ChargingStation getChargingStation() {
        return chargingStation;
    }

    public void setChargingStation(ChargingStation chargingStation) {
        this.chargingStation = chargingStation;
    }
}
