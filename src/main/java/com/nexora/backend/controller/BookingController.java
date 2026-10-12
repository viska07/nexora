
package com.nexora.backend.controller;

import com.nexora.backend.entity.Booking;
import com.nexora.backend.entity.User;
import com.nexora.backend.service.BookingService;
import com.nexora.backend.service.UserService;

import jakarta.validation.constraints.NotNull;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final UserService userService;

    public BookingController(
            BookingService bookingService,
            UserService userService
    ) {
        this.bookingService = bookingService;
        this.userService = userService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ATTENDEE')")
    public ResponseEntity<Map<String, Object>> createBooking(
            @RequestBody Map<Long, Integer> requestedTickets,
            Authentication authentication
    ) {
        User user = userService
                .getUserByEmail(authentication.getName())
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Authenticated user not found"
                        )
                );

        Booking booking = bookingService.createBooking(
                user,
                requestedTickets
        );

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", booking.getId());
        response.put("bookingCode", booking.getBookingCode());
        response.put("totalAmount", booking.getTotalAmount());
        response.put("status", booking.getStatus());
        response.put("bookedAt", booking.getBookedAt());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}