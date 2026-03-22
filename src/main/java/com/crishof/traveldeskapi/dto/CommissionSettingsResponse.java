package com.crishof.traveldeskapi.dto;

import java.math.BigDecimal;

public record CommissionSettingsResponse(
        String commissionType,
        BigDecimal commissionValue
) {
}
