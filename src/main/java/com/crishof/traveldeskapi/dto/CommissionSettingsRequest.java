package com.crishof.traveldeskapi.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CommissionSettingsRequest(
        @NotBlank(message = "Commission type is required")
        @Size(max = 30, message = "Commission type must not exceed 30 characters")
        String commissionType,

        @NotNull(message = "Commission value is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Commission value must be greater than or equal to zero")
        @Digits(integer = 8, fraction = 2, message = "Commission value format is invalid")
        BigDecimal commissionValue
) {
}
