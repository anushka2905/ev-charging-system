package com.evcharging.repository;

import com.evcharging.model.Booking;
import com.evcharging.model.User;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    
    List<Booking> findByUserId(Long userId);                   // by userId
    List<Booking> findByUser(User user);                       // by User object
    List<Booking> findByUserUsername(String username);         // by username (through User)
    Optional<Booking> findTopByUserOrderByBookingTimeDesc(User user);
    
}
