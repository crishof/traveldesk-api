package com.crishof.traveldeskapi.dto;

public record AgencySettingsResponse(
        String agencyName,
        String currency,
        String timeZone
) {
}
