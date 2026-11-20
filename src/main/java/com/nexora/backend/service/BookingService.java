package com.nexora.backend.service;

import com.nexora.backend.entity.Booking;
import com.nexora.backend.entity.BookingItem;
import com.nexora.backend.entity.Event;
import com.nexora.backend.entity.TicketType;
import com.nexora.backend.entity.User;
import com.nexora.backend.repository.BookingItemRepository;
import com.nexora.backend.repository.BookingRepository;
import com.nexora.backend.repository.TicketTypeRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingItemRepository bookingItemRepository;
    private final TicketTypeRepository ticketTypeRepository;

    public BookingService(
            BookingRepository bookingRepository,
            BookingItemRepository bookingItemRepository,
            TicketTypeRepository ticketTypeRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.bookingItemRepository = bookingItemRepository;
        this.ticketTypeRepository = ticketTypeRepository;
    }

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    public Optional<Booking> getBookingById(Long id) {
        return bookingRepository.findById(id);
    }

    public Optional<Booking> getBookingByCode(String bookingCode) {
        return bookingRepository.findByBookingCode(bookingCode);
    }

    public List<Booking> getBookingsByUser(Long userId) {
        return bookingRepository.findByUserIdOrderByBookedAtDesc(userId);
    }

    public List<BookingItem> getBookingItems(Long bookingId) {
        return bookingItemRepository.findByBookingId(bookingId);
    }

    public Booking saveBooking(Booking booking) {
        return bookingRepository.save(booking);
    }

    @Transactional
    public Booking createBooking(
            User user,
            Map<Long, Integer> requestedTickets
    ) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException(
                    "Authenticated user is required"
            );
        }

        if (requestedTickets == null || requestedTickets.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one ticket is required"
            );
        }

        /*
         * TreeMap membuat urutan ticket ID konsisten.
         * Ini juga membantu mengurangi kemungkinan deadlock
         * ketika beberapa user melakukan booking bersamaan.
         */
        Map<Long, Integer> tickets = new TreeMap<>();

        for (Map.Entry<Long, Integer> entry : requestedTickets.entrySet()) {

            Long ticketTypeId = entry.getKey();
            Integer quantity = entry.getValue();

            if (ticketTypeId == null || ticketTypeId <= 0) {
                throw new IllegalArgumentException(
                        "Ticket type ID must be valid"
                );
            }

            if (quantity == null || quantity <= 0) {
                throw new IllegalArgumentException(
                        "Ticket quantity must be greater than zero"
                );
            }

            tickets.put(ticketTypeId, quantity);
        }

        List<BookingItem> items = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        /*
         * Lock setiap ticket type sebelum membaca dan mengurangi stock.
         */
        for (Map.Entry<Long, Integer> entry : tickets.entrySet()) {

            TicketType ticketType = ticketTypeRepository
                    .findByIdForUpdate(entry.getKey())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Ticket type not found: " + entry.getKey()
                    ));

            Event event = ticketType.getEvent();

            if (event == null) {
                throw new IllegalStateException(
                        "Ticket is not associated with an event"
                );
            }

            if (!"PUBLISHED".equals(event.getStatus())) {
                throw new IllegalArgumentException(
                        "This event is not available for booking"
                );
            }

            if (event.getEventDate() == null
                    || !event.getEventDate().isAfter(LocalDate.now())) {
                throw new IllegalArgumentException(
                        "Booking for this event is closed"
                );
            }

            int quantity = entry.getValue();

            Integer available = ticketType.getAvailableQuantity();

            if (available == null || available < quantity) {
                throw new IllegalArgumentException(
                        "Insufficient ticket stock for: "
                                + ticketType.getName()
                );
            }

            BigDecimal unitPrice = ticketType.getPrice();

            if (unitPrice == null || unitPrice.signum() < 0) {
                throw new IllegalStateException(
                        "Ticket price is invalid"
                );
            }

            BigDecimal subtotal = unitPrice.multiply(
                    BigDecimal.valueOf(quantity)
            );

            BookingItem item = new BookingItem();

            item.setTicketType(ticketType);
            item.setQuantity(quantity);
            item.setUnitPrice(unitPrice);
            item.setSubtotal(subtotal);

            items.add(item);

            totalAmount = totalAmount.add(subtotal);

            ticketType.setAvailableQuantity(
                    available - quantity
            );

            ticketTypeRepository.save(ticketType);
        }

        /*
         * Create booking header.
         */
        Booking booking = new Booking();

        booking.setUser(user);
        booking.setBookingCode(generateBookingCode());
        booking.setTotalAmount(totalAmount);
        booking.setStatus("PENDING");
        booking.setBookedAt(LocalDateTime.now());

        Booking savedBooking = bookingRepository.save(booking);

        /*
         * Save booking items.
         */
        for (BookingItem item : items) {

            item.setBooking(savedBooking);

            bookingItemRepository.save(item);
        }

        return savedBooking;
    }

    private String generateBookingCode() {
        return "NEX-"
                + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 20)
                .toUpperCase();
    }
}