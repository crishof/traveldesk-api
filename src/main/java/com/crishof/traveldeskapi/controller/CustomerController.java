package com.crishof.traveldeskapi.controller;

import com.crishof.traveldeskapi.dto.CustomerRequest;
import com.crishof.traveldeskapi.dto.CustomerResponse;
import com.crishof.traveldeskapi.security.principal.SecurityUser;
import com.crishof.traveldeskapi.service.CustomerService;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Slf4j
public class CustomerController {

    private final CustomerService customerService;

    //  ===============
    //  GET CUSTOMERS
    //  ===============

    @Operation(summary = "Get customers")
    @ApiResponse(responseCode = "200", description = "Customers retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<java.util.List<CustomerResponse>> getCustomers(@AuthenticationPrincipal SecurityUser securityUser) {
        log.info("Get customers request received for userId={}", securityUser.getId());
        return ResponseEntity.ok(customerService.getAll(securityUser.getAgencyId()));
    }

    //  ===============
    //  CREATE CUSTOMER
    //  ===============

    @Operation(summary = "Create a new customer")
    @ApiResponse(responseCode = "201", description = "Customer created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(
            @AuthenticationPrincipal SecurityUser securityUser,
            @Valid @RequestBody CustomerRequest request
    ) {
        log.info("Create customer request received for userId={}, email={}", securityUser.getId(), request.email());
        return ResponseEntity.status(HttpStatusCode.valueOf(201))
                .body(customerService.create(securityUser.getAgencyId(), request));
    }

    //  ===============
    //  UPDATE CUSTOMER
    //  ===============

    @Operation(summary = "Update a customer")
    @ApiResponse(responseCode = "200", description = "Customer updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Customer not found")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @AuthenticationPrincipal SecurityUser securityUser,
            @PathVariable UUID id,
            @Valid @RequestBody CustomerRequest request
    ) {
        log.info("Update customer request received for userId={}, customerId={}", securityUser.getId(), id);
        return ResponseEntity.ok(customerService.update(securityUser.getAgencyId(), id, request));
    }

    //  ===============
    //  GET CUSTOMER BY ID
    //  ===============

    @Operation(summary = "Get a customer by ID")
    @ApiResponse(responseCode = "200", description = "Customer found successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Customer not found")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getCustomerById(
            @AuthenticationPrincipal SecurityUser securityUser,
            @PathVariable UUID id
    ) {
        log.info("Get customer by ID request received for userId={}, customerId={}", securityUser.getId(), id);
        return ResponseEntity.ok(customerService.findById(securityUser.getAgencyId(), id));
    }

    //  ===============
    //  DELETE CUSTOMER
    //  ===============

    @Operation(summary = "Delete a customer")
    @ApiResponse(responseCode = "204", description = "Customer deleted successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Customer not found")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(
            @AuthenticationPrincipal SecurityUser securityUser,
            @PathVariable UUID id
    ) {
        log.info("Delete customer request received for userId={}, customerId={}", securityUser.getId(), id);
        customerService.delete(securityUser.getAgencyId(), id);
        return ResponseEntity.noContent().build();
    }
}