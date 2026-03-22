package com.crishof.traveldeskapi.dto;

import com.crishof.traveldeskapi.model.Currency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AccountPaymentRequest(
        @NotNull(message = "Date is required")
        LocalDate date,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", inclusive = true, message = "Amount must be greater than zero")
        BigDecimal amount,

        @NotNull(message = "Currency is required")
        Currency currency,

        String description
) {
}