package com.nexora.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record BookingResponse(
        Long id,
        String bookingCode,
        Long userId,
        String userName,
        BigDecimal totalAmount,
        String status,
        LocalDateTime bookedAt,
        List<BookingItemResponse> items
) {
}