package com.crishof.traveldeskapi.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRequest(
        UUID customerId,
        BigDecimal originalAmount,
        String sourceCurrency,
        String description,
        BigDecimal exchangeRate,
        BigDecimal convertedAmount
) {
}
