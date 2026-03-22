package com.crishof.traveldeskapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AgencySettingsRequest(
        @NotBlank(message = "Agency name is required")
        @Size(max = 120, message = "Agency name must not exceed 120 characters")
        String agencyName,

        @NotBlank(message = "Currency is required")
        @Size(max = 10, message = "Currency must not exceed 10 characters")
        String currency,

        @NotBlank(message = "Time zone is required")
        @Size(max = 60, message = "Time zone must not exceed 60 characters")
        String timeZone
) {
}
