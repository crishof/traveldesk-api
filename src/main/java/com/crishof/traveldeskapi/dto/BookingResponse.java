package com.crishof.traveldeskapi.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record BookingResponse(
                UUID id,
                UUID customerId,
                String customerName,
                UUID supplierId,
                String supplierName,
                String reference,
                String description,
                BigDecimal amount,
                String currency,
                BigDecimal originalAmount,
                String sourceCurrency,
                BigDecimal exchangeRate,
                BigDecimal convertedAmount,
                LocalDate departureDate,
                LocalDate returnDate,
                LocalDate paymentDate,
                String status) {
}
