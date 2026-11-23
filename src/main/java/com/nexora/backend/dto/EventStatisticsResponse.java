package com.nexora.backend.dto;

import java.math.BigDecimal;

public record EventStatisticsResponse(
        Long eventId,
        String eventTitle,
        Integer totalBookings,
        Integer totalParticipants,
        Integer totalTicketsSold,
        BigDecimal totalRevenue
) {
}