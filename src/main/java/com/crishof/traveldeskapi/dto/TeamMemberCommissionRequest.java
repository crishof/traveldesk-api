package com.crishof.traveldeskapi.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TeamMemberCommissionRequest(
        @NotNull(message = "Commission percentage is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Commission percentage must be greater than or equal to zero")
        @DecimalMax(value = "100.0", inclusive = true, message = "Commission percentage must be less than or equal to 100")
        @Digits(integer = 3, fraction = 2, message = "Commission percentage format is invalid")
        BigDecimal commissionPercentage
) {
}