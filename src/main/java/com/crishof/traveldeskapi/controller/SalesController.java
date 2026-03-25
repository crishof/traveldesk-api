package com.crishof.traveldeskapi.controller;

import com.crishof.traveldeskapi.dto.*;
import com.crishof.traveldeskapi.security.principal.SecurityUser;
import com.crishof.traveldeskapi.service.SalesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sales")
@RequiredArgsConstructor
@Slf4j
public class SalesController {

    private final SalesService salesService;

    //  ===============
    //  GET SALES
    //  ===============

    @Operation(summary = "Get sales")
    @ApiResponse(responseCode = "200", description = "Sales retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<List<SaleResponse>> getSales(@AuthenticationPrincipal SecurityUser securityUser) {
        log.info("Get sales request received for userId={}", securityUser.getId());
        return ResponseEntity.ok(salesService.getAll(securityUser.getAgencyId()));
    }

    //  ===============
    //  CREATE SALE
    //  ===============

    @Operation(summary = "Create a new sale")
    @ApiResponse(responseCode = "201", description = "Sale created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public ResponseEntity<SaleResponse> createSale(@AuthenticationPrincipal SecurityUser securityUser, @Valid @RequestBody SaleRequest request) {
        log.info("Create sale request received: {}", request);
        return ResponseEntity.status(HttpStatusCode.valueOf(201)).body(salesService.create(securityUser.getAgencyId(), securityUser.getId(), request));
    }

    //  ===============
    //  UPDATE SALE
    //  ===============

    @Operation(summary = "Update a sale")
    @ApiResponse(responseCode = "200", description = "Sale updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Sale not found")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{id}")
    public ResponseEntity<SaleResponse> updateSale(@AuthenticationPrincipal SecurityUser securityUser, @PathVariable UUID id, @Valid @RequestBody SaleUpdateRequest request) {
        log.info("Update sale request received for userId={}, saleId={}", securityUser.getId(), id);
        return ResponseEntity.ok(salesService.update(securityUser.getAgencyId(), id, request));
    }

    //  ===============
    //  GET SALE BY ID
    //  ===============

    @Operation(summary = "Get a sale by ID")
    @ApiResponse(responseCode = "200", description = "Sale found successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Sale not found")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public ResponseEntity<SaleResponse> getSaleById(@AuthenticationPrincipal SecurityUser securityUser, @PathVariable UUID id) {
        log.info("Get sale by ID request received for userId={}, saleId={}", securityUser.getId(), id);
        return ResponseEntity.ok(salesService.findById(securityUser.getAgencyId(), id));
    }

    //  ===============
    //  DELETE SALE
    //  ===============

    @Operation(summary = "Delete a sale")
    @ApiResponse(responseCode = "204", description = "Sale deleted successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Sale not found")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSale(@AuthenticationPrincipal SecurityUser securityUser, @PathVariable UUID id) {
        log.info("Delete sale request received for userId={}, saleId={}", securityUser.getId(), id);
        salesService.delete(securityUser.getAgencyId(), id);
        return ResponseEntity.noContent().build();
    }

    //  ===============
    //  REGISTER PAYMENT
    //  ===============
    @Operation(summary = "Register a partial payment for a sale")
    @ApiResponse(responseCode = "200", description = "Payment registered successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Sale not found")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{saleId}/payments")
    public ResponseEntity<SaleResponse> registerPayment(@AuthenticationPrincipal SecurityUser securityUser, @PathVariable UUID saleId, @Valid @RequestBody PaymentRequest request) {
        log.info("Register payment request received for saleId={}, userId={}", saleId, securityUser.getId());
        log.info("Payment request: {}", request);
        return ResponseEntity.ok(salesService.registerPayment(securityUser.getAgencyId(), saleId, request));
    }

    @Operation(summary = "Get payments for a sale")
    @ApiResponse(responseCode = "200", description = "Payments retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Sale not found")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{saleId}/payments")
    public ResponseEntity<List<PaymentResponse>> getPaymentsForSale(@AuthenticationPrincipal SecurityUser securityUser, @PathVariable UUID saleId) {
        log.info("Get payments request received for saleId={}, userId={}", saleId, securityUser.getId());
        return ResponseEntity.ok(salesService.getPaymentsForSale(securityUser.getAgencyId(), saleId));
    }

    @Operation(summary = "Delete a payment for a sale")
    @ApiResponse(responseCode = "200", description = "Payment deleted successfully, updated sale returned")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Sale or payment not found")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{saleId}/payments/{paymentId}")
    public ResponseEntity<SaleResponse> deletePayment(@AuthenticationPrincipal SecurityUser securityUser, @PathVariable UUID saleId, @PathVariable UUID paymentId) {
        log.info("Delete payment request received for saleId={}, paymentId={}, userId={}", saleId, paymentId, securityUser.getId());
        return ResponseEntity.ok(salesService.deletePayment(securityUser.getAgencyId(), saleId, paymentId));
    }
}