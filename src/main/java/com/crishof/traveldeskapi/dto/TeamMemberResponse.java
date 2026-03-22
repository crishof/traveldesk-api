package com.crishof.traveldeskapi.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TeamMemberResponse(
        UUID id,
        String fullName,
        String email,
        String role,
        String status,
        BigDecimal commissionPercentage
) {
}
