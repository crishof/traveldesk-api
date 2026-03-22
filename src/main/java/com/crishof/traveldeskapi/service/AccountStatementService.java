package com.crishof.traveldeskapi.service;

import com.crishof.traveldeskapi.dto.AccountPaymentRequest;
import com.crishof.traveldeskapi.dto.AccountStatementDTO;
import com.crishof.traveldeskapi.model.Currency;
import jakarta.validation.Valid;

import java.util.UUID;

public interface AccountStatementService {

    AccountStatementDTO getStatement(UUID userId, Currency currency);

    AccountStatementDTO addPayment(UUID userId, @Valid AccountPaymentRequest request);
}
