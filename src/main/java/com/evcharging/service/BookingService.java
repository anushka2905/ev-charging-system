package com.evcharging.service;

import com.evcharging.model.Booking;
import com.evcharging.model.Booking.Status;
import com.evcharging.model.ChargingSlot;
import com.evcharging.model.User;

import java.util.List;

public interface BookingService {

    Booking createBooking(Booking booking);
    List<Booking> getAllBookings();
    Booking getBookingById(Long id);
    void deleteBooking(Long id);

    // Slot Booking
    Booking bookSlot(User user, ChargingSlot slot);
    Booking bookSlot(Long slotId, Long userId);
    Booking bookSlot(Long slotId, String username);
    void bookSlot(Long slotId, User user);

    // Payment
    void markAsPaid(Long bookingId);

    // Fetching Bookings
    List<Booking> getBookingsForUser(User user);
    List<Booking> getBookingsByUser(User user);
    List<Booking> getBookingsByUsername(String username);

    // Latest Booking (for showing Payment button)
    Booking getLatestBookingForUser(String username);

    // User
    User getUserByEmail(String email);
    List<Booking> getBookingsByUserAndStation(Long userId, Long stationId);
    List<Booking> findByUser(User user);
	List<Booking> getBookingsByUser(Long userId);

	int countBookingsByStation(Long stationId);
    int countPaidBookingsByStation(Long stationId);
    
    void updateStatus(Long bookingId, Status status);

}
