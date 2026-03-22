package com.crishof.traveldeskapi.service;

import com.crishof.traveldeskapi.dto.SupplierCreateRequest;
import com.crishof.traveldeskapi.dto.SupplierRequest;
import com.crishof.traveldeskapi.dto.SupplierResponse;
import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

public interface SupplierService {

    List<SupplierResponse> getAll(UUID agencyId);

    SupplierResponse create(UUID agencyId, @Valid SupplierCreateRequest request);

    SupplierResponse update(UUID agencyId, UUID id, @Valid SupplierRequest request);

    void delete(UUID agencyId, UUID id);

    SupplierResponse findById(UUID agencyId, UUID id);
}
