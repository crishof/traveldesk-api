package com.crishof.traveldeskapi.controller;

import com.crishof.traveldeskapi.dto.SupplierCreateRequest;
import com.crishof.traveldeskapi.dto.SupplierRequest;
import com.crishof.traveldeskapi.dto.SupplierResponse;
import com.crishof.traveldeskapi.security.principal.SecurityUser;
import com.crishof.traveldeskapi.service.SupplierService;
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

@RestController
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
@Slf4j
public class SupplierController {

    private final SupplierService supplierService;

    //  ===============
    //  GET PROVIDERS
    //  ===============

    @Operation(summary = "Get suppliers")
    @ApiResponse(responseCode = "200", description = "Suppliers retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<java.util.List<SupplierResponse>> getSuppliers(@AuthenticationPrincipal SecurityUser securityUser) {
        log.info("Get suppliers request received for userId={}", securityUser.getId());
        return ResponseEntity.ok(supplierService.getAll(securityUser.getAgencyId()));
    }

    //  ===============
    //  CREATE PROVIDER
    //  ===============

    @Operation(summary = "Create a new supplier")
    @ApiResponse(responseCode = "201", description = "Supplier created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public ResponseEntity<SupplierResponse> createSupplier(@AuthenticationPrincipal SecurityUser securityUser, @Valid @RequestBody SupplierCreateRequest request) {
        log.info("Create supplier request received for userId={}, name={}", securityUser.getId(), request.name());
        return ResponseEntity.status(HttpStatusCode.valueOf(201)).body(supplierService.create(securityUser.getAgencyId(), request));
    }

    //  ===============
    //  UPDATE PROVIDER
    //  ===============

    @Operation(summary = "Update a supplier")
    @ApiResponse(responseCode = "200", description = "Supplier updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Supplier not found")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{id}")
    public ResponseEntity<SupplierResponse> updateSupplier(@AuthenticationPrincipal SecurityUser securityUser, @PathVariable java.util.UUID id, @Valid @RequestBody SupplierRequest request) {
        log.info("Update supplier request received for userId={}, supplierId={}", securityUser.getId(), id);
        return ResponseEntity.ok(supplierService.update(securityUser.getAgencyId(), id, request));
    }

    //  ===============
    //  GET PROVIDER BY ID
    //  ===============

    @Operation(summary = "Get a supplier by ID")
    @ApiResponse(responseCode = "200", description = "Supplier found successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Supplier not found")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public ResponseEntity<SupplierResponse> getSupplierById(@AuthenticationPrincipal SecurityUser securityUser, @PathVariable java.util.UUID id) {
        log.info("Get supplier by ID request received for userId={}, supplierId={}", securityUser.getId(), id);
        return ResponseEntity.ok(supplierService.findById(securityUser.getAgencyId(), id));
    }

    //  ===============
    //  DELETE PROVIDER
    //  ===============

    @Operation(summary = "Delete a supplier")
    @ApiResponse(responseCode = "204", description = "Supplier deleted successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Supplier not found")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSupplier(@AuthenticationPrincipal SecurityUser securityUser, @PathVariable java.util.UUID id) {
        log.info("Delete supplier request received for userId={}, supplierId={}", securityUser.getId(), id);
        supplierService.delete(securityUser.getAgencyId(), id);
        return ResponseEntity.noContent().build();
    }
}