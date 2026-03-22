package com.crishof.traveldeskapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record BookingRequest(
        @NotNull(message = "Customer id is required")
        UUID customerId,

        UUID supplierId,

        @NotBlank(message = "Reference is required")
        @Size(max = 50, message = "Reference must not exceed 50 characters")
        String reference,

        @NotBlank(message = "Passenger name is required")
        @Size(max = 120, message = "Passenger name must not exceed 120 characters")
        String passengerName,

        @NotBlank(message = "Destination is required")
        @Size(max = 120, message = "Destination must not exceed 120 characters")
        String destination,

        @NotNull(message = "Amount is required")
        BigDecimal amount,

        @NotBlank(message = "Currency is required")
        @Size(max = 10, message = "Currency must not exceed 10 characters")
        String currency,

        BigDecimal originalAmount,

        @Size(max = 10, message = "Source currency must not exceed 10 characters")
        String sourceCurrency,

        BigDecimal exchangeRate,

        BigDecimal convertedAmount,

        LocalDate departureDate,

        LocalDate returnDate,

        LocalDate paymentDate,

        @NotBlank(message = "Status is required")
        @Size(max = 30, message = "Status must not exceed 30 characters")
        String status
) {
}
