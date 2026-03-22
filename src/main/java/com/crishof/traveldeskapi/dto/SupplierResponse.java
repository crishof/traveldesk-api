package com.crishof.traveldeskapi.dto;

import java.util.UUID;

public record SupplierResponse(
        UUID id,
        String name,
        String email,
        String phone,
        String serviceType
) {
}
