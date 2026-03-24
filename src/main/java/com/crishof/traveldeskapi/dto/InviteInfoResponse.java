package com.crishof.traveldeskapi.dto;

import java.time.Instant;
import java.util.UUID;

public record InviteInfoResponse(
        String email,
        String role,
        UUID agencyId,
        String agencyName,
        Instant expiresAt,
        String passwordRequirements
) {
}

