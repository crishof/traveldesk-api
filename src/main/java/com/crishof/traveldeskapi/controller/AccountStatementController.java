package com.crishof.traveldeskapi.controller;

import com.crishof.traveldeskapi.dto.AccountPaymentRequest;
import com.crishof.traveldeskapi.dto.AccountStatementDTO;
import com.crishof.traveldeskapi.model.Currency;
import com.crishof.traveldeskapi.security.principal.SecurityUser;
import com.crishof.traveldeskapi.service.AccountStatementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/account-statement")
@RequiredArgsConstructor
public class AccountStatementController {

    private final AccountStatementService accountStatementService;

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<AccountStatementDTO> getAccountStatement(
            @AuthenticationPrincipal SecurityUser securityUser,
            @RequestParam Currency currency
    ) {
        AccountStatementDTO accountStatement = accountStatementService.getStatement(securityUser.getId(), currency);
        return ResponseEntity.ok(accountStatement);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/me/payments")
    public ResponseEntity<AccountStatementDTO> addAccountPayment(
            @AuthenticationPrincipal SecurityUser securityUser,
            @Valid @RequestBody AccountPaymentRequest request
    ) {
        AccountStatementDTO accountStatement = accountStatementService.addPayment(securityUser.getId(), request);
        return ResponseEntity.ok(accountStatement);
    }
}
