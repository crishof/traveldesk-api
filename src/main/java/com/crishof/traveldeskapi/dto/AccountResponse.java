package com.crishof.traveldeskapi.dto;

public record AccountResponse(
        String fullName,
        String email,
        String status
) {
}
