package com.crishof.traveldeskapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AcceptInviteRequest(
        @NotBlank(message = "Invitation token is required")
        @Schema(description = "Invitation token received by email")
        String token,

        @NotBlank(message = "Full name is required")
        @Size(max = 120, message = "Full name must not exceed 120 characters")
        @Schema(description = "Full name for the invited user", example = "Jane Doe")
        String fullName,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&._-]).+$",
                message = "Password must contain at least one uppercase letter, " +
                        "one lowercase letter, one number and one special character"
        )
        @Schema(description = PASSWORD_REQUIREMENTS, example = "StrongPass1!")
        String password
) {
    public static final String PASSWORD_REQUIREMENTS =
            "Password must be 8-72 characters long and include at least one uppercase letter, one lowercase letter, one number and one special character (@$!%*?&._-)";
}
