package com.evcharging.dto;

public class StationSummaryDTO {
    private String name;
    private int bookedCount;
    private int paidCount;

    public StationSummaryDTO(String name, int bookedCount, int paidCount) {
        this.name = name;
        this.bookedCount = bookedCount;
        this.paidCount = paidCount;
    }

    // Getters
    public String getName() { return name; }
    public int getBookedCount() { return bookedCount; }
    public int getPaidCount() { return paidCount; }
}
