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

@Service
public class BookingServiceImpl implements BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ChargingSlotRepository chargingSlotRepository;

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

        booking.setStatus(Status.PAID);
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

    @Override
    public Booking bookSlot(Long slotId, String username) {
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

        return bookingRepository.save(booking);
    }

    @Override
    public Booking getLatestBookingForUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        return bookingRepository.findTopByUserOrderByBookingTimeDesc(user)
                .orElseThrow(() -> new RuntimeException("No recent booking found for user."));
    }

    @Override
    public Booking bookSlot(Long slotId, Long userId) {
        ChargingSlot slot = chargingSlotRepository.findById(slotId)
                .orElseThrow(() -> new RuntimeException("Slot not found"));

        if (slot.isBooked()) {
            throw new RuntimeException("Slot already booked");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        Booking booking = new Booking();
        booking.setSlot(slot);
        booking.setUser(user);
        booking.setBookingTime(LocalDateTime.now());
        booking.setStatus(Status.BOOKED);

        slot.setBooked(true);
        chargingSlotRepository.save(slot);

        return bookingRepository.save(booking);
    }

    @Override
    public Booking bookSlot(User user, ChargingSlot slot) {
        if (slot.isBooked()) {
            throw new RuntimeException("Slot already booked");
        }

        Booking booking = new Booking();
        booking.setSlot(slot);
        booking.setUser(user);
        booking.setBookingTime(LocalDateTime.now());
        booking.setStatus(Status.BOOKED);

        slot.setBooked(true);
        chargingSlotRepository.save(slot);

        return bookingRepository.save(booking);
    }


	@Override
	public void bookSlot(Long slotId, User user) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public List<Booking> getBookingsByUserAndStation(Long userId, Long stationId) {
	    return bookingRepository.findByUserIdAndSlot_ChargingStation_Id(userId, stationId);
	}
	
	
    public BookingServiceImpl(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

	@Override
	public List<Booking> findByUser(User user) {
		// TODO Auto-generated method stub
		return bookingRepository.findByUser(user);
	}

	@Override
	public List<Booking> getBookingsByUser(User user) {
		// TODO Auto-generated method stub
		 return bookingRepository.findByUser(user);
	}

	public int countBookingsByStation(Long stationId) {
        return bookingRepository.countBySlot_ChargingStation_Id(stationId);
    }

    public int countPaidBookingsByStation(Long stationId) {
        return bookingRepository.countBySlot_ChargingStation_IdAndStatus(stationId, Status.PAID);
    }

    public void updateStatus(Long bookingId, Booking.Status status) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        booking.setStatus(status);

        // If cancelled → free up the slot
        if (status == Booking.Status.CANCELLED) {
            booking.getSlot().setAvailable(true);
            chargingSlotRepository.save(booking.getSlot());
        }

        bookingRepository.save(booking);
    }

	
	
}
