package com.evcharging.service;

import com.evcharging.exception.ResourceNotFoundException;
import com.evcharging.exception.SlotAlreadyBookedException;
import com.evcharging.model.Booking;
import com.evcharging.model.Booking.Status;
import com.evcharging.model.ChargingSlot;
import com.evcharging.model.User;
import com.evcharging.repository.BookingRepository;
import com.evcharging.repository.ChargingSlotRepository;
import com.evcharging.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * BookingServiceImpl — Phase 0 refactored.
 * Cleaned up duplicate logic, proper exceptions, constructor injection.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final ChargingSlotRepository chargingSlotRepository;
    private final UserRepository userRepository;

    @Override
    public Booking createBooking(Booking booking) {
        return bookingRepository.save(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Booking getBookingById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", id));
    }

    @Override
    public void deleteBooking(Long id) {
        bookingRepository.deleteById(id);
    }

    @Override
    public void markAsPaid(Long bookingId) {
        Booking booking = getBookingById(bookingId);
        booking.setStatus(Status.PAID);
        bookingRepository.save(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Booking> getBookingsForUser(User user) {
        return bookingRepository.findByUser(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Booking> getBookingsByUser(User user) {
        return bookingRepository.findByUser(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Booking> getBookingsByUser(Long userId) {
        return bookingRepository.findByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Booking> getBookingsByUsername(String username) {
        return bookingRepository.findByUserUsername(username);
    }

    @Override
    public Booking bookSlot(Long slotId, String username) {
        ChargingSlot slot = chargingSlotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("ChargingSlot", slotId));

        if (slot.isBooked()) throw new SlotAlreadyBookedException(slotId);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User with username: " + username));

        return saveNewBooking(slot, user);
    }

    @Override
    public Booking bookSlot(Long slotId, Long userId) {
        ChargingSlot slot = chargingSlotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("ChargingSlot", slotId));

        if (slot.isBooked()) throw new SlotAlreadyBookedException(slotId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        return saveNewBooking(slot, user);
    }

    @Override
    public Booking bookSlot(User user, ChargingSlot slot) {
        if (slot.isBooked()) throw new SlotAlreadyBookedException(slot.getId());
        return saveNewBooking(slot, user);
    }

    @Override
    public void bookSlot(Long slotId, User user) {
        bookSlot(slotId, user.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Booking getLatestBookingForUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User with username: " + username));
        return bookingRepository.findTopByUserOrderByBookingTimeDesc(user)
                .orElseThrow(() -> new ResourceNotFoundException("No bookings found for user: " + username));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Booking> getBookingsByUserAndStation(Long userId, Long stationId) {
        return bookingRepository.findByUserIdAndSlot_ChargingStation_Id(userId, stationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Booking> findByUser(User user) {
        return bookingRepository.findByUser(user);
    }

    @Override
    @Transactional(readOnly = true)
    public int countBookingsByStation(Long stationId) {
        return bookingRepository.countBySlot_ChargingStation_Id(stationId);
    }

    @Override
    @Transactional(readOnly = true)
    public int countPaidBookingsByStation(Long stationId) {
        return bookingRepository.countBySlot_ChargingStation_IdAndStatus(stationId, Status.PAID);
    }

    @Override
    public void updateStatus(Long bookingId, Status status) {
        Booking booking = getBookingById(bookingId);
        booking.setStatus(status);

        if (status == Status.CANCELLED) {
            booking.getSlot().setAvailable(true);
            chargingSlotRepository.save(booking.getSlot());
        }
        bookingRepository.save(booking);
    }

    // ── Private helper ────────────────────────────────────────────

    private Booking saveNewBooking(ChargingSlot slot, User user) {
        Booking booking = Booking.builder()
                .slot(slot)
                .user(user)
                .bookingTime(LocalDateTime.now())
                .status(Status.BOOKED)
                .build();

        slot.setStatus(ChargingSlot.SlotStatus.BOOKED);
        slot.setLastBookedAt(LocalDateTime.now());
        chargingSlotRepository.save(slot);

        Booking saved = bookingRepository.save(booking);
        log.info("Booking created: id={} slot={} user={}", saved.getId(), slot.getSlotName(), user.getUsername());
        return saved;
    }
}
