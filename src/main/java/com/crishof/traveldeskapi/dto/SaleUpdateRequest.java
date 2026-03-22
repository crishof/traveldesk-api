package com.crishof.traveldeskapi.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record SaleUpdateRequest(
        UUID customerId,
        String customerName,
        @Size(max = 120, message = "Destination must not exceed 120 characters")
        String destination,
        @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be greater than zero")
        BigDecimal amount,
        @Size(max = 10, message = "Currency must not exceed 10 characters")
        String currency,
        @Size(max = 30, message = "Status must not exceed 30 characters")
        String status,
        LocalDate departureDate,
        String description
) {
}