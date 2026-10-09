package com.nexora.backend.service;

import com.nexora.backend.entity.BookingItem;
import com.nexora.backend.repository.BookingItemRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookingItemService {

    private final BookingItemRepository bookingItemRepository;

    public BookingItemService(BookingItemRepository bookingItemRepository) {
        this.bookingItemRepository = bookingItemRepository;
    }

    public List<BookingItem> getAllBookingItems() {
        return bookingItemRepository.findAll();
    }

    public List<BookingItem> getItemsByBooking(Long bookingId) {
        return bookingItemRepository.findByBookingId(bookingId);
    }

    public BookingItem saveBookingItem(BookingItem bookingItem) {
        return bookingItemRepository.save(bookingItem);
    }
}