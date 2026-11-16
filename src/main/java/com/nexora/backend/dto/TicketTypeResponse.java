package com.nexora.backend.dto;

import java.math.BigDecimal;

public record TicketTypeResponse(
        Long id,
        Long eventId,
        String name,
        BigDecimal price,
        Integer quota,
        Integer availableQuantity
) {
}
