package com.nexora.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminEventStatusRequest(

        @NotBlank(message = "Status is required")
        String status

) {
}    