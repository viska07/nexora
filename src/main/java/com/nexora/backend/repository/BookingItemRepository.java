package com.nexora.backend.repository;

import com.nexora.backend.entity.BookingItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookingItemRepository extends JpaRepository<BookingItem, Long> {

    List<BookingItem> findByBookingId(Long bookingId);

    @Query("""
            SELECT bi
            FROM BookingItem bi
            JOIN FETCH bi.booking b
            JOIN FETCH bi.ticketType tt
            JOIN FETCH tt.event e
            WHERE e.id = :eventId
            """)
    List<BookingItem> findByEventId(
            @Param("eventId") Long eventId
    );
}