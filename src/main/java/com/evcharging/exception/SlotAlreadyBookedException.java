package com.evcharging.exception;

public class SlotAlreadyBookedException extends RuntimeException {
    public SlotAlreadyBookedException(Long slotId) {
        super("Charging slot " + slotId + " is already booked.");
    }
    public SlotAlreadyBookedException(String message) {
        super(message);
    }
}
