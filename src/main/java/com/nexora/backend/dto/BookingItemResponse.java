package com.nexora.backend.dto;

import java.math.BigDecimal;

public record BookingItemResponse(
        Long id,
        Long ticketTypeId,
        String ticketTypeName,
        Long eventId,
        String eventTitle,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {
}