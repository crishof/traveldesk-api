package com.crishof.traveldeskapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ThemeRequest(
        @NotBlank(message = "Mode is required")
        @Size(max = 20, message = "Mode must not exceed 20 characters")
        String mode,

        @NotBlank(message = "Primary color is required")
        @Size(max = 20, message = "Primary color must not exceed 20 characters")
        String primaryColor,

        @NotBlank(message = "Secondary color is required")
        @Size(max = 20, message = "Secondary color must not exceed 20 characters")
        String secondaryColor
) {
}
