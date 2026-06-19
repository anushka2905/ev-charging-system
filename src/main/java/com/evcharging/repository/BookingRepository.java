package com.evcharging.repository;

import com.evcharging.model.Booking;
import com.evcharging.model.Booking.Status;
import com.evcharging.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * BookingRepository — Phase 0 enhanced version.
 * Added analytics queries for Phase 6 predictive analytics.
 */
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // ── Legacy (backward-compat) ──────────────────────────────────
    List<Booking> findByUserId(Long userId);
    List<Booking> findByUser(User user);
    List<Booking> findByUserUsername(String username);
    Optional<Booking> findTopByUserOrderByBookingTimeDesc(User user);
    List<Booking> findByUserIdAndSlot_ChargingStation_Id(Long userId, Long stationId);
    Booking findTopByUser_EmailOrderByBookingTimeDesc(String email);
    int countBySlot_ChargingStation_Id(Long stationId);
    int countBySlot_ChargingStation_IdAndStatus(Long stationId, Status status);

    // ── Phase 0: Status-based queries ─────────────────────────────
    List<Booking> findByStatus(Status status);
    List<Booking> findByUserAndStatus(User user, Status status);

    // ── Phase 6: Predictive Analytics queries ─────────────────────

    /** Count bookings per hour for a station (for peak-hour prediction) */
    @Query("SELECT HOUR(b.bookingTime) AS hour, COUNT(b) AS cnt " +
           "FROM Booking b WHERE b.slot.chargingStation.id = :stationId " +
           "GROUP BY HOUR(b.bookingTime) ORDER BY cnt DESC")
    List<Object[]> findBookingsByHourForStation(@Param("stationId") Long stationId);

    /** Count bookings per day of week (0=Sunday … 6=Saturday) */
    @Query("SELECT DAYOFWEEK(b.bookingTime) AS dow, COUNT(b) AS cnt " +
           "FROM Booking b GROUP BY DAYOFWEEK(b.bookingTime) ORDER BY dow")
    List<Object[]> findBookingsByDayOfWeek();

    /** Bookings in a date range (for demand forecasting) */
    @Query("SELECT b FROM Booking b WHERE b.bookingTime BETWEEN :start AND :end ORDER BY b.bookingTime")
    List<Booking> findBookingsInRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /** Station utilization — booked count vs total for each station */
    @Query("SELECT s.id, s.name, COUNT(b) " +
           "FROM ChargingStation s LEFT JOIN ChargingSlot sl ON sl.chargingStation.id = s.id " +
           "LEFT JOIN Booking b ON b.slot.id = sl.id AND b.status IN ('BOOKED','COMPLETED','PAID') " +
           "GROUP BY s.id, s.name ORDER BY COUNT(b) DESC")
    List<Object[]> findStationUtilizationStats();

    /** Recent bookings for a user — for AI chatbot context */
    @Query("SELECT b FROM Booking b WHERE b.user.username = :username " +
           "ORDER BY b.bookingTime DESC")
    List<Booking> findRecentByUsername(@Param("username") String username,
                                       org.springframework.data.domain.Pageable pageable);

    /** Total revenue from PAID bookings */
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.paymentStatus = 'SUCCESS'")
    Double getTotalRevenue();

    /** Phase 6: bookings count per city */
    @Query("SELECT s.city, COUNT(b) FROM Booking b " +
           "JOIN b.slot sl JOIN sl.chargingStation s " +
           "GROUP BY s.city ORDER BY COUNT(b) DESC")
    List<Object[]> findBookingCountByCity();
}
