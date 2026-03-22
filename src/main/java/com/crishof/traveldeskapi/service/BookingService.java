package com.crishof.traveldeskapi.service;

import com.crishof.traveldeskapi.dto.BookingRequest;
import com.crishof.traveldeskapi.dto.BookingResponse;
import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

public interface BookingService {

    List<BookingResponse> getAll(UUID agencyId);

    BookingResponse create(UUID agencyId, UUID userId, @Valid BookingRequest request);

    BookingResponse update(UUID agencyId, UUID id, @Valid BookingRequest request);

    void delete(UUID agencyId, UUID id);

    BookingResponse findById(UUID agencyId, UUID id);
}
