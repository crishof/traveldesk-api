package com.crishof.traveldeskapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to reset user password using reset token")
public record ResetPasswordRequest(
        @NotBlank(message = "Reset token is required")
        @Schema(description = "Password reset token received via email",
                example = "8f5c2e7a-1c23-4b3f-9f44-2b6d2b4a8d99")
        String token,

        @NotBlank(message = "New password is required")
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&._-]).+$",
                message = "Password must contain at least one uppercase letter, one lowercase letter, one number and one special character"
        )
        @Schema(description = "New password. Must contain uppercase, lowercase, number and special character",
                example = "NewSecure123!")
        String newPassword,

        @NotBlank(message = "Password confirmation is required")
        @Schema(description = "Confirmation of the new password",
                example = "NewSecure123!")
        String confirmPassword
) {
}
