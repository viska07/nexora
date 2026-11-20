package com.nexora.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TicketTypeResponse(
        Long id,
        Long eventId,
        String name,
        String description,
        BigDecimal price,
        Integer quota,
        Integer availableQuantity,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}