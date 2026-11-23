package com.nexora.backend.dto;

import java.time.LocalDateTime;

public record AdminUserResponse(
        Long id,
        String fullName,
        String email,
        Long roleId,
        String roleName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}