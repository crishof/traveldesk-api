package com.crishof.traveldeskapi.dto;

import com.crishof.traveldeskapi.model.User;

import java.util.UUID;

public record AuthResponse(
        UUID userId,
        String fullName,
        String email,
        String role,
        String status,
        String accessToken,
        String refreshToken,
        String tokenType
) {
    public static AuthResponse from(User user, String accessToken, String refreshToken) {
        return new AuthResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().name(),
                user.getStatus().name(),
                accessToken,
                refreshToken,
                "Bearer"
        );
    }
}