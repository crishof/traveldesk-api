package com.crishof.traveldeskapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TeamMemberRequest(
        @NotBlank(message = "Role is required")
        @Size(max = 30, message = "Role must not exceed 30 characters")
        String role,

        @NotBlank(message = "Status is required")
        @Size(max = 30, message = "Status must not exceed 30 characters")
        String status
) {
}
