package com.evcharging.service;

import com.evcharging.model.Booking;
import com.evcharging.model.Booking.Status;
import com.evcharging.model.ChargingSlot;
import com.evcharging.model.User;
import com.evcharging.repository.BookingRepository;
import com.evcharging.repository.ChargingSlotRepository;
import com.evcharging.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class BookingServiceImpl implements BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ChargingSlotRepository slotRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public Booking createBooking(Booking booking) {
        return bookingRepository.save(booking);
    }

    @Override
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    @Override
    public Booking getBookingById(Long id) {
        return bookingRepository.findById(id).orElse(null);
    }

    @Override
    public void deleteBooking(Long id) {
        bookingRepository.deleteById(id);
    }


    @Override
    public void markAsPaid(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        booking.setStatus(Booking.Status.PAID);
        bookingRepository.save(booking);
    }
    
    @Override
    public List<Booking> getBookingsForUser(User user) {
        return bookingRepository.findByUser(user);
    }

    @Override
    public List<Booking> getBookingsByUser(Long userId) {
        return bookingRepository.findByUserId(userId);
    }

    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public List<Booking> getBookingsByUsername(String username) {
        return bookingRepository.findByUserUsername(username);
    }
    
    @Autowired
    private ChargingSlotRepository chargingSlotRepository;
    

    @Override
    public void bookSlot(Long slotId, String username) {
        ChargingSlot slot = chargingSlotRepository.findById(slotId)
            .orElseThrow(() -> new RuntimeException("Slot not found"));

        if (slot.isBooked()) {
            throw new RuntimeException("Slot already booked");
        }

        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found with username: " + username));

        Booking booking = new Booking();
        booking.setSlot(slot);
        booking.setUser(user);
        booking.setBookingTime(LocalDateTime.now());
        booking.setStatus(Status.BOOKED);

        slot.setBooked(true);
        chargingSlotRepository.save(slot);
        bookingRepository.save(booking);
    }



    
    @Override
    public Booking getLatestBookingForUser(String username) {
        User user = userRepository.findByEmail(username);
        return bookingRepository.findTopByUserOrderByBookingTimeDesc(user)
                .orElseThrow(() -> new RuntimeException("No recent booking found."));
    }

	@Override
	public Booking bookSlot(User user, ChargingSlot slot) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Booking bookSlot(Long slotId, Long userId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void bookSlot(Long slotId, User user) {
		// TODO Auto-generated method stub
		
	}

    
    
}
