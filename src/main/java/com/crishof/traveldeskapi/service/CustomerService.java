package com.crishof.traveldeskapi.service;

import com.crishof.traveldeskapi.dto.CustomerRequest;
import com.crishof.traveldeskapi.dto.CustomerResponse;
import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

public interface CustomerService {

    List<CustomerResponse> getAll(UUID agencyId);

    CustomerResponse create(UUID agencyId, @Valid CustomerRequest request);

    CustomerResponse update(UUID agencyId, UUID id, @Valid CustomerRequest request);

    void delete(UUID agencyId, UUID id);

    CustomerResponse findById(UUID agencyId, UUID id);
}
