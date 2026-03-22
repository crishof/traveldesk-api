package com.crishof.traveldeskapi.dto;

import com.crishof.traveldeskapi.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateInvitationRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email format is invalid")
        @Size(max = 150,
                message = "Email must not exceed 150 characters")
        String email,

        @NotNull(message = "Role is required")
        Role role
) {
}
