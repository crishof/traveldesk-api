package com.crishof.traveldeskapi.service;

import com.crishof.traveldeskapi.dto.*;
import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

public interface SalesService {

    List<SaleResponse> getAll(UUID agencyId);

    SaleResponse create(UUID agencyId, UUID userId, @Valid SaleRequest request);

    SaleResponse update(UUID agencyId, UUID id, @Valid SaleUpdateRequest request);

    void delete(UUID agencyId, UUID id);

    SaleResponse findById(UUID agencyId, UUID id);

    SaleResponse registerPayment(UUID agencyId, UUID saleId, @Valid PaymentRequest request);

    List<PaymentResponse> getPaymentsForSale(UUID agencyId, UUID saleId);

    SaleResponse deletePayment(UUID agencyId, UUID saleId, UUID paymentId);
}