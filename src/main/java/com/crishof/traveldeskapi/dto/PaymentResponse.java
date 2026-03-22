package com.crishof.traveldeskapi.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        BigDecimal originalAmount,
        String sourceCurrency,
        String description,
        BigDecimal exchangeRate,
        BigDecimal convertedAmount,
        Instant paymentDate
) {
}
