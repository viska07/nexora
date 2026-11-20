package com.nexora.backend.controller;

import com.nexora.backend.dto.BookingItemResponse;
import com.nexora.backend.dto.BookingResponse;
import com.nexora.backend.entity.Booking;
import com.nexora.backend.entity.BookingItem;
import com.nexora.backend.entity.User;
import com.nexora.backend.service.BookingService;
import com.nexora.backend.service.UserService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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

    /**
     * Create booking.
     *
     * Request format:
     *
     * {
     *   "1": 2,
     *   "3": 1
     * }
     *
     * Key   = ticket type ID
     * Value = quantity
     */
    @PostMapping
    @PreAuthorize("hasRole('ATTENDEE')")
    public ResponseEntity<BookingResponse> createBooking(
            @RequestBody Map<Long, Integer> requestedTickets,
            Authentication authentication
    ) {
        User user = getAuthenticatedUser(authentication);

        Booking booking = bookingService.createBooking(
                user,
                requestedTickets
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(toResponse(booking));
    }

    /**
     * Get all bookings belonging to the authenticated attendee.
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('ATTENDEE')")
    public ResponseEntity<List<BookingResponse>> getMyBookings(
            Authentication authentication
    ) {
        User user = getAuthenticatedUser(authentication);

        List<BookingResponse> response = bookingService
                .getBookingsByUser(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    /**
     * Get booking detail.
     *
     * Attendee can only access their own booking.
     * Admin can access any booking.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ATTENDEE', 'ADMIN')")
    public ResponseEntity<?> getBookingById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Booking booking = bookingService
                .getBookingById(id)
                .orElse(null);

        if (booking == null) {
            return ResponseEntity.notFound().build();
        }

        User authenticatedUser = getAuthenticatedUser(authentication);
        String role = getRole(authentication);

        if (role.equals("ROLE_ATTENDEE")
                && !booking.getUser().getId()
                .equals(authenticatedUser.getId())) {

            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "message",
                            "You can only access your own bookings"
                    ));
        }

        return ResponseEntity.ok(toResponse(booking));
    }

    /**
     * Get booking by booking code.
     *
     * Attendee can only access their own booking.
     * Admin can access any booking.
     */
    @GetMapping("/code/{bookingCode}")
    @PreAuthorize("hasAnyRole('ATTENDEE', 'ADMIN')")
    public ResponseEntity<?> getBookingByCode(
            @PathVariable String bookingCode,
            Authentication authentication
    ) {
        Booking booking = bookingService
                .getBookingByCode(bookingCode)
                .orElse(null);

        if (booking == null) {
            return ResponseEntity.notFound().build();
        }

        User authenticatedUser = getAuthenticatedUser(authentication);
        String role = getRole(authentication);

        if (role.equals("ROLE_ATTENDEE")
                && !booking.getUser().getId()
                .equals(authenticatedUser.getId())) {

            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "message",
                            "You can only access your own bookings"
                    ));
        }

        return ResponseEntity.ok(toResponse(booking));
    }

    private User getAuthenticatedUser(
            Authentication authentication
    ) {
        return userService
                .getUserByEmail(authentication.getName())
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Authenticated user not found"
                        )
                );
    }

    private String getRole(Authentication authentication) {
        return authentication.getAuthorities()
                .stream()
                .map(authority -> authority.getAuthority())
                .findFirst()
                .orElse("");
    }

    private BookingResponse toResponse(Booking booking) {

        List<BookingItemResponse> items = bookingService
                .getBookingItems(booking.getId())
                .stream()
                .map(this::toItemResponse)
                .toList();

        return new BookingResponse(
                booking.getId(),
                booking.getBookingCode(),
                booking.getUser().getId(),
                booking.getUser().getFullName(),
                booking.getTotalAmount(),
                booking.getStatus(),
                booking.getBookedAt(),
                items
        );
    }

    private BookingItemResponse toItemResponse(
            BookingItem item
    ) {
        return new BookingItemResponse(
                item.getId(),
                item.getTicketType().getId(),
                item.getTicketType().getName(),
                item.getTicketType().getEvent().getId(),
                item.getTicketType().getEvent().getTitle(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getSubtotal()
        );
    }
}