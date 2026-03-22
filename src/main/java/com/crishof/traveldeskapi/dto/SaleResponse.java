package com.crishof.traveldeskapi.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SaleResponse(
        UUID id,
        UUID customerId,
        String customerName,
        UUID agentId,
        String destination,
        BigDecimal amount,
        String currency,
        String status,
        BigDecimal paidAmount,
        LocalDate departureDate,
        Instant createdAt,
        BigDecimal commissionPercentage
) {
}
