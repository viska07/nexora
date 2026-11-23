package com.nexora.backend.repository;

import com.nexora.backend.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByBookingCode(String bookingCode);

    List<Booking> findByUserId(Long userId);

    List<Booking> findByUserIdOrderByBookedAtDesc(Long userId);

    @Query("""
            SELECT DISTINCT b
            FROM Booking b
            JOIN BookingItem bi ON bi.booking = b
            JOIN bi.ticketType tt
            JOIN tt.event e
            WHERE e.id = :eventId
            ORDER BY b.bookedAt DESC
            """)
    List<Booking> findBookingsByEventId(
            @Param("eventId") Long eventId
    );
}