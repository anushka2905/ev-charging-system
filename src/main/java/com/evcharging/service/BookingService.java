package com.evcharging.service;

import com.evcharging.model.Booking;
import com.evcharging.model.ChargingSlot;
import com.evcharging.model.User;

import java.util.List;

public interface BookingService {

    Booking createBooking(Booking booking);
    List<Booking> getAllBookings();
    Booking getBookingById(Long id);
    void deleteBooking(Long id);
    Booking bookSlot(User user, ChargingSlot slot);
    void markAsPaid(Long bookingId);
    List<Booking> getBookingsForUser(User user);
    List<Booking> getBookingsByUser(Long userId);
    User getUserByEmail(String email);
	Booking bookSlot(Long slotId, Long userId);
	List<Booking> getBookingsByUsername(String username);
    void bookSlot(Long slotId, String username);
    Booking getLatestBookingForUser(String username);
	void bookSlot(Long slotId, User user);

    
}
