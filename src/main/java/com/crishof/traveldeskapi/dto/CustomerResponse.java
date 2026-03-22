package com.crishof.traveldeskapi.dto;

import java.util.UUID;

public record CustomerResponse(
        UUID id,
        String fullName,
        String email,
        String phone
) {
}
